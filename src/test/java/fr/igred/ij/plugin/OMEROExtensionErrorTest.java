/*
 *  Copyright (C) 2021 GReD
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package fr.igred.ij.plugin;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


@ExtendWith(TestResultLogger.class)
class OMEROExtensionErrorTest {

    private final ByteArrayOutputStream outContent  = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent  = new ByteArrayOutputStream();
    private final PrintStream           originalOut = System.out;
    private final PrintStream           originalErr = System.err;

    private OMEROMacroExtension ext;


    @BeforeEach
    public void setUp() {
        ext = new OMEROMacroExtension();
        Object[] args = {"omero", 4064d, "testUser", "password"};
        ext.handleExtension("connectToOMERO", args);
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }


    @AfterEach
    public void tearDown() {
        ext.handleExtension("disconnect", null);
        System.setOut(originalOut);
        System.setErr(originalErr);
    }


    @Test
    void testRun() {
        ext.run("");
        String expected = "Cannot install extensions from outside a macro!";
        assertEquals(expected, outContent.toString().trim());
    }


    @Test
    void testNoSuchMethod() {
        ext.handleExtension("hello", null);
        String expected = "No such method: hello";
        assertEquals(expected, outContent.toString().trim());
    }


    @Test
    void testListForUserError() {
        Object[] args = {"hello"};
        ext.handleExtension("listForUser", args);
        String expected = "Could not retrieve user: hello";
        assertEquals(expected, outContent.toString().trim());
    }


    @Test
    void testSudoError() {
        Object[] args = {"roger"};
        ext.handleExtension("sudo", args);
        String expected = "Could not switch user: null";
        assertEquals(expected, outContent.toString().trim());
    }


    @Test
    void testEndSudoError() {
        ext.handleExtension("endSudo", null);
        String expected = "No sudo has been used before.";
        assertEquals(expected, outContent.toString().trim());
    }


    @Test
    void testListInvalidArgs() {
        Object[] args = {"dataset", null, 2.0};
        ext.handleExtension("list", args);
        String expected = "Second argument should not be null.";
        assertEquals(expected, outContent.toString().trim());
    }


    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {"tag;hello;2",
                                         "project;hello;2",
                                         "dataset;hello;2",
                                         "image;hello;2",
                                         "hello;tag;2",
                                         "hello;TestDatasetImport;",
                                         "hello;;",})
    void testListInvalidType(String type1, String type2, Double id) {
        Object[] args   = {type1, type2, id};
        String   output = ext.handleExtension("list", args);
        String   error  = outContent.toString().trim();
        assertTrue(output.isEmpty());
        assertTrue(error.startsWith("Invalid type: hello. Possible values are:"));
    }


    @ParameterizedTest
    @ValueSource(strings = {"link", "unlink"})
    void testLinkUnlinkInvalidType(String function) {
        Object[] args = {"tag", 2.0, "hello", 2.0};
        ext.handleExtension(function, args);
        String expected = "Invalid type: hello.";
        assertEquals(expected, outContent.toString().trim());
    }


    @ParameterizedTest
    @ValueSource(strings = {"link", "unlink"})
    void testCannotLinkOrUnlink(String function) {
        Object[] args = {"hello", 2.0, "world", 2.0};
        ext.handleExtension(function, args);
        String expected = String.format("Cannot %s hello and world", function);
        assertEquals(expected, outContent.toString().trim());
    }

}
