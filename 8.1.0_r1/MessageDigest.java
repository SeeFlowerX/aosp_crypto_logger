/*
 * Copyright (c) 1996, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package java.security;

import org.json.JSONObject;

import java.util.*;
import java.lang.*;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.InputStream;
import java.io.ByteArrayInputStream;

import java.nio.ByteBuffer;

import javax.crypto.AndroidBase64;
import javax.crypto.ContextHolder;
import javax.crypto.MyUtil;

/**
 * This MessageDigest class provides applications the functionality of a
 * message digest algorithm, such as SHA-1 or SHA-256.
 * Message digests are secure one-way hash functions that take arbitrary-sized
 * data and output a fixed-length hash value.
 *
 * <p>A MessageDigest object starts out initialized. The data is
 * processed through it using the {@link #update(byte) update}
 * methods. At any point {@link #reset() reset} can be called
 * to reset the digest. Once all the data to be updated has been
 * updated, one of the {@link #digest() digest} methods should
 * be called to complete the hash computation.
 *
 * <p>The {@code digest} method can be called once for a given number
 * of updates. After {@code digest} has been called, the MessageDigest
 * object is reset to its initialized state.
 *
 * <p>Implementations are free to implement the Cloneable interface.
 * Client applications can test cloneability by attempting cloning
 * and catching the CloneNotSupportedException:
 *
 * <pre>{@code
 * MessageDigest md = MessageDigest.getInstance("SHA");
 *
 * try {
 *     md.update(toChapter1);
 *     MessageDigest tc1 = md.clone();
 *     byte[] toChapter1Digest = tc1.digest();
 *     md.update(toChapter2);
 *     ...etc.
 * } catch (CloneNotSupportedException cnse) {
 *     throw new DigestException("couldn't make digest of partial content");
 * }
 * }</pre>
 *
 * <p>Note that if a given implementation is not cloneable, it is
 * still possible to compute intermediate digests by instantiating
 * several instances, if the number of digests is known in advance.
 *
 * <p>Note that this class is abstract and extends from
 * {@code MessageDigestSpi} for historical reasons.
 * Application developers should only take notice of the methods defined in
 * this {@code MessageDigest} class; all the methods in
 * the superclass are intended for cryptographic service providers who wish to
 * supply their own implementations of message digest algorithms.
 *
 * <p> Android provides the following <code>MessageDigest</code> algorithms:
 * <table>
 *   <thead>
 *     <tr>
 *       <th>Algorithm</th>
 *       <th>Supported API Levels</th>
 *     </tr>
 *   </thead>
 *   <tbody>
 *     <tr>
 *       <td>MD5</td>
 *       <td>1+</td>
 *     </tr>
 *     <tr>
 *       <td>SHA-1</td>
 *       <td>1+</td>
 *     </tr>
 *     <tr>
 *       <td>SHA-224</td>
 *       <td>1-8,22+</td>
 *     </tr>
 *     <tr>
 *       <td>SHA-256</td>
 *       <td>1+</td>
 *     </tr>
 *     <tr>
 *       <td>SHA-384</td>
 *       <td>1+</td>
 *     </tr>
 *     <tr>
 *       <td>SHA-512</td>
 *       <td>1+</td>
 *     </tr>
 *   </tbody>
 * </table>
 *
 * These algorithms are described in the <a href=
 * "{@docRoot}openjdk-redirect.html?v=8&path=/technotes/guides/security/StandardNames.html#MessageDigest">
 * MessageDigest section</a> of the
 * Java Cryptography Architecture Standard Algorithm Name Documentation.
 *
 * @author Benjamin Renaud
 *
 * @see DigestInputStream
 * @see DigestOutputStream
 */

public abstract class MessageDigest extends MessageDigestSpi {

    // Android-removed: this debugging mechanism is not used in Android.
    /*
    private static final Debug pdebug =
                        Debug.getInstance("provider", "Provider");
    private static final boolean skipDebug =
        Debug.isOn("engine=") && !Debug.isOn("messagedigest");
    */

    private String algorithm;

    // The state of this digest
    private static final int INITIAL = 0;
    private static final int IN_PROGRESS = 1;
    private int state = INITIAL;

    // The provider
    private Provider provider;


    //add by icew4y 2019 12 13
    private JSONObject jsoninfo = new JSONObject();
    //private StringBuffer infos = new StringBuffer();
    private ArrayList<Byte> tmpBytes = new ArrayList<>();
    //add by icew4y 2019 12 13

    private static boolean switch_state = true;
    private String monPackageName = "";

    /**
     * Lower case Hex Digits.
     */
    private static final String HEX_DIGITS = "0123456789abcdef";

    /**
     * Byte mask.
     */
    private static final int BYTE_MSK = 0xFF;

    /**
     * Hex digit mask.
     */
    private static final int HEX_DIGIT_MASK = 0xF;

    /**
     * Number of bits per Hex digit (4).
     */
    private static final int HEX_DIGIT_BITS = 4;

    public static String toHexString(final byte[] byteArray) {
        StringBuilder sb = new StringBuilder(byteArray.length * 2);
        for (int i = 0; i < byteArray.length; i++) {
            int b = byteArray[i] & BYTE_MSK;
            sb.append(HEX_DIGITS.charAt(b >>> HEX_DIGIT_BITS)).append(
                    HEX_DIGITS.charAt(b & HEX_DIGIT_MASK));
        }
        return sb.toString();
    }

    public static String byteArrayToString(byte[] input) {
        if(input==null)
            return "";
        String out = new String(input);
        int tmp = 0;
        for (int i = 0; i < out.length(); i++) {
            int c = out.charAt(i);

            if (c >= 32 && c < 127) {
                tmp++;
            }
        }

        if (tmp > (out.length() * 0.60)) {
            StringBuilder sb = new StringBuilder();
            for (byte b : input) {
                if (b >= 32 && b < 127)
                    sb.append(String.format("%c", b));
                else
                    sb.append('.');
            }
            out = sb.toString();

        } else {
            out = AndroidBase64.encodeToString(input, AndroidBase64.NO_WRAP);
        }

        return out;
    }



    private static synchronized void priter(String content, String packageName) {
        //System.out.println(content);
        MyUtil.appendFile("/data/data/" + packageName + "/MessageDigest", content + "\r\n");
    }

    //add by icew4y

    /**
     * Creates a message digest with the specified algorithm name.
     *
     * @param algorithm the standard name of the digest algorithm.
     * See the MessageDigest section in the <a href=
     * "{@docRoot}openjdk-redirect.html?v=8&path=/technotes/guides/security/StandardNames.html#MessageDigest">
     * Java Cryptography Architecture Standard Algorithm Name Documentation</a>
     * for information about standard algorithm names.
     */
    protected MessageDigest(String algorithm) {
        this.algorithm = algorithm;
    }

    /**
     * Returns a MessageDigest object that implements the specified digest
     * algorithm.
     *
     * <p> This method traverses the list of registered security Providers,
     * starting with the most preferred Provider.
     * A new MessageDigest object encapsulating the
     * MessageDigestSpi implementation from the first
     * Provider that supports the specified algorithm is returned.
     *
     * <p> Note that the list of registered providers may be retrieved via
     * the {@link Security#getProviders() Security.getProviders()} method.
     *
     * @param algorithm the name of the algorithm requested.
     * See the MessageDigest section in the <a href=
     * "{@docRoot}openjdk-redirect.html?v=8&path=/technotes/guides/security/StandardNames.html#MessageDigest">
     * Java Cryptography Architecture Standard Algorithm Name Documentation</a>
     * for information about standard algorithm names.
     *
     * @return a Message Digest object that implements the specified algorithm.
     *
     * @exception NoSuchAlgorithmException if no Provider supports a
     *          MessageDigestSpi implementation for the
     *          specified algorithm.
     *
     * @see Provider
     */
    public static MessageDigest getInstance(String algorithm)
    throws NoSuchAlgorithmException {
        try {
            MessageDigest md;
            Object[] objs = Security.getImpl(algorithm, "MessageDigest",
                                             (String)null);
            if (objs[0] instanceof MessageDigest) {
                md = (MessageDigest)objs[0];
            } else {
                md = new Delegate((MessageDigestSpi)objs[0], algorithm);
            }
            md.provider = (Provider)objs[1];

            // Android-removed: this debugging mechanism is not used in Android.
            /*
            if (!skipDebug && pdebug != null) {
                pdebug.println("MessageDigest." + algorithm +
                    " algorithm from: " + md.provider.getName());
            }
            */

            return md;

        } catch(NoSuchProviderException e) {
            throw new NoSuchAlgorithmException(algorithm + " not found");
        }
    }

    /**
     * Returns a MessageDigest object that implements the specified digest
     * algorithm.
     *
     * <p> A new MessageDigest object encapsulating the
     * MessageDigestSpi implementation from the specified provider
     * is returned.  The specified provider must be registered
     * in the security provider list.
     *
     * <p> Note that the list of registered providers may be retrieved via
     * the {@link Security#getProviders() Security.getProviders()} method.
     *
     * @param algorithm the name of the algorithm requested.
     * See the MessageDigest section in the <a href=
     * "{@docRoot}openjdk-redirect.html?v=8&path=/technotes/guides/security/StandardNames.html#MessageDigest">
     * Java Cryptography Architecture Standard Algorithm Name Documentation</a>
     * for information about standard algorithm names.
     *
     * @param provider the name of the provider.
     *
     * @return a MessageDigest object that implements the specified algorithm.
     *
     * @exception NoSuchAlgorithmException if a MessageDigestSpi
     *          implementation for the specified algorithm is not
     *          available from the specified provider.
     *
     * @exception NoSuchProviderException if the specified provider is not
     *          registered in the security provider list.
     *
     * @exception IllegalArgumentException if the provider name is null
     *          or empty.
     *
     * @see Provider
     */
    public static MessageDigest getInstance(String algorithm, String provider)
        throws NoSuchAlgorithmException, NoSuchProviderException
    {
        if (provider == null || provider.length() == 0)
            throw new IllegalArgumentException("missing provider");
        Object[] objs = Security.getImpl(algorithm, "MessageDigest", provider);
        if (objs[0] instanceof MessageDigest) {
            MessageDigest md = (MessageDigest)objs[0];
            md.provider = (Provider)objs[1];
            return md;
        } else {
            MessageDigest delegate =
                new Delegate((MessageDigestSpi)objs[0], algorithm);
            delegate.provider = (Provider)objs[1];
            return delegate;
        }
    }

    /**
     * Returns a MessageDigest object that implements the specified digest
     * algorithm.
     *
     * <p> A new MessageDigest object encapsulating the
     * MessageDigestSpi implementation from the specified Provider
     * object is returned.  Note that the specified Provider object
     * does not have to be registered in the provider list.
     *
     * @param algorithm the name of the algorithm requested.
     * See the MessageDigest section in the <a href=
     * "{@docRoot}openjdk-redirect.html?v=8&path=/technotes/guides/security/StandardNames.html#MessageDigest">
     * Java Cryptography Architecture Standard Algorithm Name Documentation</a>
     * for information about standard algorithm names.
     *
     * @param provider the provider.
     *
     * @return a MessageDigest object that implements the specified algorithm.
     *
     * @exception NoSuchAlgorithmException if a MessageDigestSpi
     *          implementation for the specified algorithm is not available
     *          from the specified Provider object.
     *
     * @exception IllegalArgumentException if the specified provider is null.
     *
     * @see Provider
     *
     * @since 1.4
     */
    public static MessageDigest getInstance(String algorithm,
                                            Provider provider)
        throws NoSuchAlgorithmException
    {
        if (provider == null)
            throw new IllegalArgumentException("missing provider");
        Object[] objs = Security.getImpl(algorithm, "MessageDigest", provider);
        if (objs[0] instanceof MessageDigest) {
            MessageDigest md = (MessageDigest)objs[0];
            md.provider = (Provider)objs[1];
            return md;
        } else {
            MessageDigest delegate =
                new Delegate((MessageDigestSpi)objs[0], algorithm);
            delegate.provider = (Provider)objs[1];
            return delegate;
        }
    }

    /**
     * Returns the provider of this message digest object.
     *
     * @return the provider of this message digest object
     */
    public final Provider getProvider() {
        return this.provider;
    }

    /**
     * Updates the digest using the specified byte.
     *
     * @param input the byte with which to update the digest.
     */
    public void update(byte input) {
        engineUpdate(input);
        state = IN_PROGRESS;


        //add by icew4y 2019 12 14
        //System.out.println("update(byte arg0)");
        //if (switch_state == true && !MyUtil.check_oom(tmpBytes)) {
            try {
                String packageName = ContextHolder.getPackageName();
                if (!packageName.equals("")) {
                    if (!MyUtil.isWhiteList(packageName)) {
                        if (monPackageName.equals("")) {
                            monPackageName = MyUtil.readPackageNameFromFile();
                        }
                        if (!monPackageName.equals("")) {
                           if (monPackageName.contains(packageName)) {
                                tmpBytes.add(input);
                            }
                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        //}

        //add by icew4y 2019 12 14
    }

    /**
     * Updates the digest using the specified array of bytes, starting
     * at the specified offset.
     *
     * @param input the array of bytes.
     *
     * @param offset the offset to start from in the array of bytes.
     *
     * @param len the number of bytes to use, starting at
     * {@code offset}.
     */
    public void update(byte[] input, int offset, int len) {
        if (input == null) {
            throw new IllegalArgumentException("No input buffer given");
        }
        if (input.length - offset < len) {
            throw new IllegalArgumentException("Input buffer too short");
        }
        engineUpdate(input, offset, len);
        state = IN_PROGRESS;

        //add by icew4y 2019 12 13

        //System.out.println("update(byte[] input, int offset, int len)\n" + MyUtil.getCurrentStackTrack(Thread.currentThread().getStackTrace()));
        //if (switch_state == true && !MyUtil.check_oom(tmpBytes) && (len < MyUtil.LIMIT_SIZE)) {
            try {

                String packageName = ContextHolder.getPackageName();
                if (!packageName.equals("")) {
                    if (!MyUtil.isWhiteList(packageName)) {
                        if (monPackageName.equals("")) {
                            monPackageName = MyUtil.readPackageNameFromFile();
                        }
                        if (!monPackageName.equals("")) {
                           if (monPackageName.contains(packageName)) {

                                byte[] realdata = new byte[len];
                                System.arraycopy(input, offset, realdata, 0, len);
                                for (byte b : realdata) {
                                    tmpBytes.add(b);
                                }
                            }
                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        //}

        //add by icew4y 2019 12 13
    }

    /**
     * Updates the digest using the specified array of bytes.
     *
     * @param input the array of bytes.
     */
    public void update(byte[] input) {
        engineUpdate(input, 0, input.length);
        state = IN_PROGRESS;

        //add by icew4y 2019 12 13
        //System.out.println("update(byte[] input)\n" + MyUtil.getCurrentStackTrack(Thread.currentThread().getStackTrace()));
        //if (switch_state == true && !MyUtil.check_oom(tmpBytes) && !MyUtil.check_oom(input)) {

            try {
                String packageName = ContextHolder.getPackageName();
                if (!packageName.equals("")) {
                    if (!MyUtil.isWhiteList(packageName)) {
                        if (monPackageName.equals("")) {
                            monPackageName = MyUtil.readPackageNameFromFile();
                        }
                        if (!monPackageName.equals("")) {
                           if (monPackageName.contains(packageName)) {
                                for (byte b : input) {
                                    tmpBytes.add(b);
                                }
                            }
                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        //}
        //add by icew4y 2019 12 13
    }

    /**
     * Update the digest using the specified ByteBuffer. The digest is
     * updated using the {@code input.remaining()} bytes starting
     * at {@code input.position()}.
     * Upon return, the buffer's position will be equal to its limit;
     * its limit will not have changed.
     *
     * @param input the ByteBuffer
     * @since 1.5
     */
    public final void update(ByteBuffer input) {
        if (input == null) {
            throw new NullPointerException();
        }
        engineUpdate(input);
        state = IN_PROGRESS;

        //add by icew4y 2019 12 13

        //System.out.println("update(ByteBuffer input)");
        //if (switch_state == true && !MyUtil.check_oom(tmpBytes) && !MyUtil.check_oom(input)) {
            try {

                String packageName = ContextHolder.getPackageName();
                if (!packageName.equals("")) {
                    if (!MyUtil.isWhiteList(packageName)) {
                        if (monPackageName.equals("")) {
                            monPackageName = MyUtil.readPackageNameFromFile();
                        }
                        if (!monPackageName.equals("")) {
                           if (monPackageName.contains(packageName)) {
                                byte[] t = input.array();
                                for (byte b : t) {
                                    tmpBytes.add(b);
                                }
                            }
                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        //}
        //add by icew4y 2019 12 13
    }

    /**
     * Completes the hash computation by performing final operations
     * such as padding. The digest is reset after this call is made.
     *
     * @return the array of bytes for the resulting hash value.
     */
    public byte[] digest() {
        /* Resetting is the responsibility of implementors. */
        byte[] result = engineDigest();
        state = INITIAL;
        //return result;

        //add by icew4y 2019 12 13

        //System.out.println("calling -> digest()");

        //在这里怎么读取到调用者的包名？
        //if (switch_state == true && !MyUtil.check_oom(tmpBytes)) {
            try {

                String packageName = ContextHolder.getPackageName();
                if (!packageName.equals("")) {
                    if (!MyUtil.isWhiteList(packageName)) {
                        if (monPackageName.equals("")) {
                            monPackageName = MyUtil.readPackageNameFromFile();
                        }
                        if (!monPackageName.equals("")) {
                           if (monPackageName.contains(packageName)) {


                                jsoninfo.put("Algorithm", getAlgorithm());
                                Provider provider_ = getProvider();
                                if (provider_ != null) {
                                    jsoninfo.put("Provider", provider_.getName());
                                }


                                StringBuffer tmpsb = new StringBuffer();
                                if (tmpBytes.size() > 0) {
                                    int n = tmpBytes.size();
                                    byte[] resultBytes = new byte[n];
                                    for (int i = 0; i < n; i++) {
                                        resultBytes[i] = (byte) tmpBytes.get(i);
                                    }

                                    jsoninfo.put("data", byteArrayToString(resultBytes));
                                    jsoninfo.put("Base64Data", AndroidBase64.encodeToString(resultBytes, AndroidBase64.NO_WRAP));


                                } else {
                                    jsoninfo.put("data", "");
                                }

                                jsoninfo.put("digest", toHexString(result));
                                jsoninfo.put("StackTrace", AndroidBase64.encodeToString(MyUtil.getCurrentStackTrack(Thread.currentThread().getStackTrace()).getBytes(), AndroidBase64.NO_WRAP));

                                priter("MessageDigestTag:" + jsoninfo.toString(), packageName);
                                jsoninfo = new JSONObject();
                                tmpBytes.clear();
                            }
                        }
                    }

                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        //}
        return result;
        //add by icew4y 2019 12 13
    }

    /**
     * Completes the hash computation by performing final operations
     * such as padding. The digest is reset after this call is made.
     *
     * @param buf output buffer for the computed digest
     *
     * @param offset offset into the output buffer to begin storing the digest
     *
     * @param len number of bytes within buf allotted for the digest
     *
     * @return the number of bytes placed into {@code buf}
     *
     * @exception DigestException if an error occurs.
     */
    public int digest(byte[] buf, int offset, int len) throws DigestException {
        if (buf == null) {
            throw new IllegalArgumentException("No output buffer given");
        }
        if (buf.length - offset < len) {
            throw new IllegalArgumentException
                ("Output buffer too small for specified offset and length");
        }
//        int numBytes = engineDigest(buf, offset, len);
        state = INITIAL;
        //return numBytes;

        //add by icew4y 2019 12 13
        //System.out.println("digest(byte[] buf, int offset, int len)");
        int result = engineDigest(buf, offset, len);
        //if (switch_state == true && !MyUtil.check_oom(tmpBytes)) {

            try {
                String packageName = ContextHolder.getPackageName();

                if (!packageName.equals("")) {
                    if (!MyUtil.isWhiteList(packageName)) {
                        if (monPackageName.equals("")) {
                            monPackageName = MyUtil.readPackageNameFromFile();
                        }

                        if (!monPackageName.equals("")) {
                           if (monPackageName.contains(packageName)) {

                                jsoninfo.put("Algorithm", getAlgorithm());
                                Provider provider_ = getProvider();
                                if (provider_ != null) {
                                    jsoninfo.put("Provider", provider_.getName());
                                }


                                StringBuffer tmpsb = new StringBuffer();
                                if (tmpBytes.size() > 0) {
                                    int n = tmpBytes.size();
                                    byte[] resultBytes = new byte[n];
                                    for (int i = 0; i < n; i++) {
                                        resultBytes[i] = (byte) tmpBytes.get(i);
                                    }

                                    jsoninfo.put("data", byteArrayToString(resultBytes));
                                    jsoninfo.put("Base64Data", AndroidBase64.encodeToString(resultBytes, AndroidBase64.NO_WRAP));


                                } else {
                                    jsoninfo.put("data", "");
                                }


                                //数据
                                byte[] readresult = new byte[len];
                                System.arraycopy(buf, offset, readresult, 0, len);
                                jsoninfo.put("digest", toHexString(readresult));
                                jsoninfo.put("StackTrace", AndroidBase64.encodeToString(MyUtil.getCurrentStackTrack(Thread.currentThread().getStackTrace()).getBytes(), AndroidBase64.NO_WRAP));

                                priter("MessageDigestTag:" + jsoninfo.toString(), packageName);
                                jsoninfo = new JSONObject();
                                tmpBytes.clear();
                            }
                        }
                    }


                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        //}

        return result;
        //add by icew4y 2019 12 13
    }

    /**
     * Performs a final update on the digest using the specified array
     * of bytes, then completes the digest computation. That is, this
     * method first calls {@link #update(byte[]) update(input)},
     * passing the <i>input</i> array to the {@code update} method,
     * then calls {@link #digest() digest()}.
     *
     * @param input the input to be updated before the digest is
     * completed.
     *
     * @return the array of bytes for the resulting hash value.
     */
    public byte[] digest(byte[] input) {
        update(input);
        return digest();
    }

    /**
     * Returns a string representation of this message digest object.
     */
    public String toString() {
        // BEGIN Android-changed: Use StringBuilder instead of a ByteArrayOutputStream.
        StringBuilder builder = new StringBuilder();
        builder.append(algorithm);
        builder.append(" Message Digest from ");
        builder.append(provider.getName());
        builder.append(", ");

        switch (state) {
        case INITIAL:
            builder.append("<initialized>");
            break;
        case IN_PROGRESS:
            builder.append("<in progress>");
            break;
        }

        return builder.toString();
        // END Android-changed: Use StringBuilder instead of a ByteArrayOutputStream.
    }

    /**
     * Compares two digests for equality. Does a simple byte compare.
     *
     * @param digesta one of the digests to compare.
     *
     * @param digestb the other digest to compare.
     *
     * @return true if the digests are equal, false otherwise.
     */
    public static boolean isEqual(byte[] digesta, byte[] digestb) {
        if (digesta == digestb) return true;
        if (digesta == null || digestb == null) {
            return false;
        }
        if (digesta.length != digestb.length) {
            return false;
        }

        int result = 0;
        // time-constant comparison
        for (int i = 0; i < digesta.length; i++) {
            result |= digesta[i] ^ digestb[i];
        }
        return result == 0;
    }

    /**
     * Resets the digest for further use.
     */
    public void reset() {
        engineReset();
        state = INITIAL;

        //add by icew4y 2019 12 14
        tmpBytes.clear();
        jsoninfo = new JSONObject();
        //add by icew4y 2019 12 14
    }

    /**
     * Returns a string that identifies the algorithm, independent of
     * implementation details. The name should be a standard
     * Java Security name (such as "SHA", "MD5", and so on).
     * See the MessageDigest section in the <a href=
     * "{@docRoot}openjdk-redirect.html?v=8&path=/technotes/guides/security/StandardNames.html#MessageDigest">
     * Java Cryptography Architecture Standard Algorithm Name Documentation</a>
     * for information about standard algorithm names.
     *
     * @return the name of the algorithm
     */
    public final String getAlgorithm() {
        return this.algorithm;
    }

    /**
     * Returns the length of the digest in bytes, or 0 if this operation is
     * not supported by the provider and the implementation is not cloneable.
     *
     * @return the digest length in bytes, or 0 if this operation is not
     * supported by the provider and the implementation is not cloneable.
     *
     * @since 1.2
     */
    public final int getDigestLength() {
        int digestLen = engineGetDigestLength();
        if (digestLen == 0) {
            try {
                MessageDigest md = (MessageDigest)clone();
                byte[] digest = md.digest();
                return digest.length;
            } catch (CloneNotSupportedException e) {
                return digestLen;
            }
        }
        return digestLen;
    }

    /**
     * Returns a clone if the implementation is cloneable.
     *
     * @return a clone if the implementation is cloneable.
     *
     * @exception CloneNotSupportedException if this is called on an
     * implementation that does not support {@code Cloneable}.
     */
    public Object clone() throws CloneNotSupportedException {
        if (this instanceof Cloneable) {
            return super.clone();
        } else {
            throw new CloneNotSupportedException();
        }
    }




    /*
     * The following class allows providers to extend from MessageDigestSpi
     * rather than from MessageDigest. It represents a MessageDigest with an
     * encapsulated, provider-supplied SPI object (of type MessageDigestSpi).
     * If the provider implementation is an instance of MessageDigestSpi,
     * the getInstance() methods above return an instance of this class, with
     * the SPI object encapsulated.
     *
     * Note: All SPI methods from the original MessageDigest class have been
     * moved up the hierarchy into a new class (MessageDigestSpi), which has
     * been interposed in the hierarchy between the API (MessageDigest)
     * and its original parent (Object).
     */

    static class Delegate extends MessageDigest {

        // The provider implementation (delegate)
        private MessageDigestSpi digestSpi;

        // constructor
        public Delegate(MessageDigestSpi digestSpi, String algorithm) {
            super(algorithm);
            this.digestSpi = digestSpi;
        }

        /**
         * Returns a clone if the delegate is cloneable.
         *
         * @return a clone if the delegate is cloneable.
         *
         * @exception CloneNotSupportedException if this is called on a
         * delegate that does not support {@code Cloneable}.
         */
        public Object clone() throws CloneNotSupportedException {
            if (digestSpi instanceof Cloneable) {
                MessageDigestSpi digestSpiClone =
                    (MessageDigestSpi)digestSpi.clone();
                // Because 'algorithm', 'provider', and 'state' are private
                // members of our supertype, we must perform a cast to
                // access them.
                MessageDigest that =
                    new Delegate(digestSpiClone,
                                 ((MessageDigest)this).algorithm);
                that.provider = ((MessageDigest)this).provider;
                that.state = ((MessageDigest)this).state;
                return that;
            } else {
                throw new CloneNotSupportedException();
            }
        }

        protected int engineGetDigestLength() {
            return digestSpi.engineGetDigestLength();
        }

        protected void engineUpdate(byte input) {
            digestSpi.engineUpdate(input);
        }

        protected void engineUpdate(byte[] input, int offset, int len) {
            digestSpi.engineUpdate(input, offset, len);
        }

        protected void engineUpdate(ByteBuffer input) {
            digestSpi.engineUpdate(input);
        }

        protected byte[] engineDigest() {
            return digestSpi.engineDigest();
        }

        protected int engineDigest(byte[] buf, int offset, int len)
            throws DigestException {
                return digestSpi.engineDigest(buf, offset, len);
        }

        protected void engineReset() {
            digestSpi.engineReset();
        }
    }
}
