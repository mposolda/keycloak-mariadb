/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.test;

import java.net.URI;
import java.util.Date;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.UserRepresentation;

/**
 * Assumes that 2 keycloak cluster nodes are up and running on "http://node1:8080/auth" and "http://node2:8080/auth" and user "admin"
 * with password "admin" available on master realm
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UsersClusterTest {

    public static final String NODE1_URL = "http://node1:8080/auth";
    public static final String NODE2_URL = "http://node2:8080/auth";

    public static final String ADMIN_USER = "admin";
    public static final String ADMIN_PASSWORD = "admin";

    private Keycloak adminClient1;
    private Keycloak adminClient2;

    @Before
    public void before() {
        adminClient1 = Keycloak.getInstance(NODE1_URL, "master", ADMIN_USER, ADMIN_PASSWORD, Constants.ADMIN_CLI_CLIENT_ID);
        adminClient2 = Keycloak.getInstance(NODE2_URL, "master", ADMIN_USER, ADMIN_PASSWORD, Constants.ADMIN_CLI_CLIENT_ID);
    }

    @After
    public void after() {
        adminClient1.close();
        adminClient2.close();
    }

    /**
     * Rigourous Test :-)
     */
    @Test
    public void testUsersOnBothNodes()  {
        for (int i=0 ; i<10 ; i++) {
            testUser("user" + i, "user" + i + "@keycloak.org");
        }
    }

    private void testUser(String username, String email) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setEmail(email);

        // Create user on node1
        Response response = adminClient1.realm("master").users().create(user);
        Assert.assertEquals(201, response.getStatus());
        String userID = getCreatedId(response);

        println("Created " + username + " on node1");

        // Assert user immediatelly available on node2
        UserRepresentation found = adminClient2.realm("master").users().get(userID).toRepresentation();
        Assert.assertEquals(userID, found.getId());
        Assert.assertEquals(username, found.getUsername());
        Assert.assertEquals(email, found.getEmail());

        println("Verified " + username + " on node2");

        // Update user on node2
        found.setFirstName("John");
        adminClient2.realm("master").users().get(userID).update(found);

        // Check updated on node1
        found = adminClient1.realm("master").users().get(userID).toRepresentation();
        Assert.assertEquals(username, found.getUsername());
        Assert.assertEquals("John", found.getFirstName());

        // Remove on node1
        adminClient1.realm("master").users().get(userID).remove();

        // Check removed on node2
        try {
            adminClient2.realm("master").users().get(userID).toRepresentation();
            Assert.fail("Not expected to find " + username);
        } catch (NotFoundException notFoundException) {
            // Expected
        }

        println("Removed user " + username);

    }

    public static String getCreatedId(Response response) {
        URI location = response.getLocation();
        if (!response.getStatusInfo().equals(Response.Status.CREATED)) {
            Response.StatusType statusInfo = response.getStatusInfo();
            throw new RuntimeException("Create method returned status " +
                    statusInfo.getReasonPhrase() + " (Code: " + statusInfo.getStatusCode() + "); expected status: Created (201)");
        }
        if (location == null) {
            return null;
        }
        String path = location.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    private void println(String message) {
        System.out.println(new Date() + " - " + message);
    }
}
