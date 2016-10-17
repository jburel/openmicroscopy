/*
 *------------------------------------------------------------------------------
 *  Copyright (C) 2006-2010 University of Dundee. All rights reserved.
 *
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *------------------------------------------------------------------------------
 */
package integration;

import java.util.Arrays;
import java.util.List;

import ome.io.nio.RomioPixelBuffer;
import omero.api.RawPixelsStorePrx;
import omero.model.Image;
import omero.model.Pixels;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Collections of tests for the <code>RawPixelsStore</code> service.
 *
 * @author Jean-Marie Burel &nbsp;&nbsp;&nbsp;&nbsp; <a
 *         href="mailto:j.burel@dundee.ac.uk">j.burel@dundee.ac.uk</a>
 * @author Donald MacDonald &nbsp;&nbsp;&nbsp;&nbsp; <a
 *         href="mailto:donald@lifesci.dundee.ac.uk"
 *         >donald@lifesci.dundee.ac.uk</a>
 * @version 3.0 <small> (<b>Internal version:</b> $Revision: $Date: $) </small>
 * @since 3.0-Beta4
 */
public class RawPixelsStoreTest extends AbstractServerTest {
    private RawPixelsStorePrx svc;

    private int planeSize;

    private long totalSize;

    /**
     * Prepares an array of bytes.
     *
     * @param size
     *            The size of the array.
     * @param start
     *            The start value.
     * @return See above.
     */
    private byte[] prepareTestByteArray(int size, int start) {
        byte[] buf = new byte[size];
        for (int i = 0; i < buf.length; i++) {
            buf[i] = (byte) (i + start);
        }
        return buf;
    }

    @BeforeMethod
    public void localSetUp() throws Exception {
        Image image = mmFactory.createImage(ModelMockFactory.SIZE_X,
                ModelMockFactory.SIZE_Y, ModelMockFactory.SIZE_Z,
                ModelMockFactory.SIZE_T, 1);
        image = (Image) iUpdate.saveAndReturnObject(image);
        Pixels pixels = image.getPrimaryPixels();
        planeSize = pixels.getSizeX().getValue() * pixels.getSizeY().getValue();
        planeSize = planeSize * 2; // UINT16
        totalSize = planeSize * pixels.getSizeZ().getValue()
                * pixels.getSizeC().getValue() * pixels.getSizeT().getValue();
        svc = factory.createRawPixelsStore();
        svc.setPixelsId(pixels.getId().getValue(), false);
    }

    /**
     * Tests to set a plane and retrieve it, this method will test the
     * <code>setPlane</code> and <code>getPlane</code>.
     *
     * @throws Exception
     *             Thrown if an error occurred.
     * @see RawFileStoreTest#testUploadFile()
     */
    @Test
    public void testSetGetPlane() throws Exception {
        byte[] data = prepareTestByteArray(
                RomioPixelBuffer.safeLongToInteger(svc.getPlaneSize()), 0);
        svc.setPlane(data, 0, 0, 0);
        byte[] r = svc.getPlane(0, 0, 0);
        Assert.assertNotNull(r);
        Assert.assertEquals(data.length, r.length);
        Assert.assertEquals(sha1(data), sha1(r));
    }

    /**
     * Tests to set a plane and retrieve it as a hypercube, this method will
     * test the <code>setPlane</code> and <code>getHypercube</code>.
     *
     * @throws Exception
     *             Thrown if an error occurred.
     * @see RawFileStoreTest#testUploadFile()
     */
    @Test
    public void testSetPlaneGetHypercube() throws Exception {
        byte[] data = prepareTestByteArray(
                RomioPixelBuffer.safeLongToInteger(svc.getPlaneSize()), 0);
        svc.setPlane(data, 0, 0, 0);
        List<Integer> offset = Arrays.asList(new Integer[] { 0, 0, 0, 0, 0 });
        List<Integer> size = Arrays.asList(new Integer[] {
                ModelMockFactory.SIZE_X, ModelMockFactory.SIZE_Y, 1, 1, 1 });
        List<Integer> step = Arrays.asList(new Integer[] { 1, 1, 1, 1, 1 });
        byte[] r = svc.getHypercube(offset, size, step);
        Assert.assertNotNull(r);
        Assert.assertEquals(data.length, r.length);
        Assert.assertEquals(sha1(data), sha1(r));
    }

    /**
     * Tests to set a series of planes and retrieve them as a hypercube, this
     * method will test the <code>setPlane</code> and <code>getHypercube</code>.
     *
     * @throws Exception
     *             Thrown if an error occurred.
     * @see RawFileStoreTest#testUploadFile()
     */
    @Test
    public void testSetEveryPlaneGetHypercube() throws Exception {
        byte[] data = prepareTestByteArray(
                RomioPixelBuffer.safeLongToInteger(svc.getPlaneSize()), 0);
        for (int t = 0; t < ModelMockFactory.SIZE_T; t++) {
            for (int c = 0; c < 1; c++) {
                for (int z = 0; z < ModelMockFactory.SIZE_Z; z++) {
                    svc.setPlane(data, z, c, t);
                }
            }
        }
        List<Integer> offset = Arrays.asList(new Integer[] { 0, 0, 0, 0, 0 });
        List<Integer> size = Arrays.asList(new Integer[] {
                ModelMockFactory.SIZE_X, ModelMockFactory.SIZE_Y,
                ModelMockFactory.SIZE_Z, 1, ModelMockFactory.SIZE_T });
        List<Integer> step = Arrays.asList(new Integer[] { 1, 1, 1, 1, 1 });
        byte[] r = svc.getHypercube(offset, size, step);
        Assert.assertNotNull(r);
        Assert.assertEquals(data.length * ModelMockFactory.SIZE_Z
                * ModelMockFactory.SIZE_T, r.length);
    }

    /**
     * Tests the <code>getPlaneSize</code> method is accurate.
     *
     * @throws Exception
     *             Thrown if an error occurred.
     */
    @Test
    public void testPlaneSize() throws Exception {
        Assert.assertEquals(planeSize, svc.getPlaneSize());
    }

    /**
     * Tests to set a whole plane as a region with the buffer larger than the
     * plane and retrieve it, this method will test the <code>setRegion</code>
     * and <code>getPlane</code> methods.
     *
     * @throws Exception
     *             Thrown if an error occurred.
     */
    @Test
    public void testSetPlaneOffEndAsRegion() throws Exception {
        byte[] plane1 = new byte[planeSize * 2];
        plane1[0] = 1;
        plane1[planeSize - 1] = 1;
        svc.setRegion(planeSize, 0, plane1);
        byte[] plane2 = svc.getPlane(0, 0, 0);
        Assert.assertNotNull(plane1);
        Assert.assertNotNull(plane2);
        Assert.assertEquals(planeSize, plane2.length);
        Assert.assertEquals(sha1(plane1, 0, planeSize), sha1(plane2));
    }

    /**
     * Tests to set a whole plane as a region and retrieve it, this method will
     * test the <code>setRegion</code> and <code>getPlane</code> methods.
     */
    @Test
    public void testSetPlaneAsRegion() throws Exception {
        byte[] plane1 = new byte[planeSize];
        plane1[0] = 1;
        plane1[planeSize - 1] = 1;
        svc.setRegion(planeSize, 0, plane1);
        byte[] plane2 = svc.getPlane(0, 0, 0);
        Assert.assertNotNull(plane1);
        Assert.assertNotNull(plane2);
        Assert.assertEquals(plane1.length, plane2.length);
        Assert.assertEquals(sha1(plane1), sha1(plane2));
    }

    /**
     * Tests to set a region and retrieve it, this method will test the
     * <code>setRegion</code> and <code>getRegion</code> methods.
     */
    @Test
    public void testSetGetRegion() throws Exception {
        int half = planeSize / 2;
        byte[] a1 = prepareTestByteArray(half, 0);
        byte[] b1 = prepareTestByteArray(half, half);
        svc.setRegion(a1.length, 0, a1);
        svc.setRegion(b1.length, half, b1);
        byte[] a2 = svc.getRegion(a1.length, 0);
        byte[] b2 = svc.getRegion(b1.length, half);
        Assert.assertNotNull(a2);
        Assert.assertNotNull(b2);
        Assert.assertEquals(a1.length, a2.length);
        Assert.assertEquals(b1.length, b2.length);
        Assert.assertEquals(sha1(a1), sha1(a2));
        Assert.assertEquals(sha1(b1), sha1(b2));
    }

    /**
     * Tests to set a region off the end of the file.
     *
     * @throws Exception
     *             Thrown if an error occurred.
     */
    @Test
    public void testSetRegionOffEndOfFile() throws Exception {
        byte[] a1 = prepareTestByteArray(planeSize, 0);
        long offset = svc.getPlaneOffset(ModelMockFactory.SIZE_Z - 1, 0,
                ModelMockFactory.SIZE_T - 1);
        offset += (planeSize / 2);
        int remaining = (int) (totalSize - offset);
        svc.setRegion(remaining, offset, a1);
        byte[] a2 = svc.getRegion(remaining, offset);
        a1 = prepareTestByteArray(planeSize / 2, 0);
        Assert.assertNotNull(a2);
        Assert.assertEquals(remaining, a2.length);
        Assert.assertEquals(sha1(a1), sha1(a2));
    }

    /**
     * Tests to set a region off the end of plane.
     *
     * @throws Exception
     *             Thrown if an error occurred.
     */
    @Test
    public void testSetRegionOffEndOfPlane() throws Exception {
        byte[] a1 = prepareTestByteArray(planeSize, 0);
        long offset = svc.getPlaneOffset(ModelMockFactory.SIZE_Z - 2, 0,
                ModelMockFactory.SIZE_T - 1);
        offset += (planeSize / 2);
        int remaining = (int) (totalSize - planeSize - offset);
        svc.setRegion(remaining, offset, a1);
        byte[] lastPlane = svc.getPlane(ModelMockFactory.SIZE_Z - 1, 0,
                ModelMockFactory.SIZE_T - 1);
        byte[] a2 = svc.getRegion(remaining, offset);
        a1 = prepareTestByteArray(planeSize / 2, 0);
        Assert.assertNotNull(a2);
        Assert.assertNotNull(lastPlane);
        Assert.assertEquals(remaining, a2.length);
        Assert.assertEquals(planeSize, lastPlane.length);
        Assert.assertEquals(sha1(a1), sha1(a2));
        Assert.assertEquals(sha1(new byte[planeSize]), sha1(lastPlane));
    }

    /**
     * Tests to set a region off the end of plane and doesn't overwrite the
     * current content of the off the end plane.
     *
     * @throws Exception
     *             Thrown if an error occurred.
     */
    @Test
    public void testSetRegionDoesntOverwrite() throws Exception {
        byte[] a1 = prepareTestByteArray(planeSize, 0);
        long offset = svc.getPlaneOffset(ModelMockFactory.SIZE_Z - 2, 0,
                ModelMockFactory.SIZE_T - 1);
        long lastPlaneOffset = svc.getPlaneOffset(ModelMockFactory.SIZE_Z - 1,
                0, ModelMockFactory.SIZE_T - 1);
        offset += (planeSize / 2);
        int remaining = (int) (totalSize - planeSize - offset);
        svc.setRegion(1, lastPlaneOffset, new byte[] { 0x01 });
        svc.setRegion(remaining, offset, a1);
        byte[] lastPlane = svc.getPlane(ModelMockFactory.SIZE_Z - 1, 0,
                ModelMockFactory.SIZE_T - 1);
        byte[] a2 = svc.getRegion(remaining, offset);
        a1 = prepareTestByteArray(planeSize / 2, 0);
        Assert.assertNotNull(a2);
        Assert.assertNotNull(lastPlane);
        Assert.assertEquals(remaining, a2.length);
        Assert.assertEquals(planeSize, lastPlane.length);
        Assert.assertEquals(0x01, lastPlane[0]);
        Assert.assertEquals(sha1(a1), sha1(a2));
    }

    /**
     * Tests to set a region off the end of plane and doesn't overwrite the
     * current content of the off the end plane.
     *
     * @throws Exception
     *             Thrown if an error occurred.
     */
    @Test
    public void testSetRegionEveryPlane() throws Exception {
        byte[] buf = new byte[planeSize * 2];
        byte i = 1;
        long planeOffset;
        for (int t = 0; t < ModelMockFactory.SIZE_T; t++) {
            for (int c = 0; c < 1; c++) {
                for (int z = 0; z < ModelMockFactory.SIZE_Z; z++) {
                    planeOffset = svc.getPlaneOffset(z, c, t);
                    buf[0] = i;
                    buf[planeSize / 4] = i;
                    buf[planeSize / 2] = i;
                    buf[planeSize - 1] = i;
                    svc.setRegion(planeSize, planeOffset, buf);
                    i++;
                }
            }
        }
        i = 1;
        for (int t = 0; t < ModelMockFactory.SIZE_T; t++) {
            for (int c = 0; c < 1; c++) {
                for (int z = 0; z < ModelMockFactory.SIZE_Z; z++) {
                    buf = svc.getPlane(z, c, t);
                    Assert.assertEquals(planeSize, buf.length);
                    Assert.assertEquals(i, buf[0]);
                    Assert.assertEquals(i, buf[planeSize / 4]);
                    Assert.assertEquals(i, buf[planeSize / 2]);
                    Assert.assertEquals(i, buf[planeSize - 1]);
                    i++;
                }
            }
        }
    }

    /**
     * Tests to set a region that is bigger than the entire file
     *
     * @throws Exception
     *             Thrown if an error occurred.
     */
    @Test
    public void testSetMegabyteRegion() throws Exception {
        byte[] buf = new byte[1048576];
        int bufSize = buf.length;
        Assert.assertTrue(bufSize > totalSize);

        byte i = 1;
        long planeOffset;
        for (int t = 0; t < ModelMockFactory.SIZE_T; t++) {
            for (int c = 0; c < 1; c++) {
                for (int z = 0; z < ModelMockFactory.SIZE_Z; z++) {
                    planeOffset = svc.getPlaneOffset(z, c, t);
                    // manually set some values within the part of buf that
                    // will form each plane
                    buf[0] = i;
                    buf[planeSize / 4] = i;
                    buf[planeSize / 2] = i;
                    buf[planeSize - 1] = i;
                    svc.setRegion(planeSize, planeOffset, buf);
                    i++;
                }
            }
        }
        i = 1;
        for (int t = 0; t < ModelMockFactory.SIZE_T; t++) {
            for (int c = 0; c < 1; c++) {
                for (int z = 0; z < ModelMockFactory.SIZE_Z; z++) {
                    buf = svc.getPlane(z, c, t);
                    Assert.assertEquals(planeSize, buf.length);
                    Assert.assertEquals(i, buf[0]);
                    Assert.assertEquals(i, buf[planeSize / 4]);
                    Assert.assertEquals(i, buf[planeSize / 2]);
                    Assert.assertEquals(i, buf[planeSize - 1]);
                    i++;
                }
            }
        }
    }

}
