/*
 * Copyright  2003-2004 The Apache Software Foundation.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.swssf.ext;

import org.w3c.dom.Element;

import javax.security.auth.callback.Callback;

/**
 * class lent from apache wss4j
 */

/**
 * Simple class to provide a password callback mechanism.
 * <p/>
 * It uses the JAAS authentication mechanisms and callback methods.
 * In addition to the identifier (user name) this class also provides
 * information what type of information the callback <code>handle</code>
 * method shall provide.
 * <p/>
 * The <code> WSPasswordCallback</code> class defines the following usage
 * codes:
 * <ul>
 * <li><code>UNKNOWN</code> - an unknown usage. Never used by the WSS4J
 * implementation and shall be treated as an error by the <code>handle
 * </code> method.</li>
 * <li><code>DECRYPT</code> - need a password to get the private key of
 * this identifier (username) from the keystore. WSS4J uses this private
 * key to decrypt the session (symmetric) key. Because the encryption
 * method uses the public key to encrypt the session key it needs no
 * password (a public key is usually not protected by a password).</li>
 * <li><code>USERNAME_TOKEN</code> - need the password to fill in or to
 * verify a <code>UsernameToken</code>.</li>
 * <li><code>SIGNATURE</code> - need the password to get the private key of
 * this identifier (username) from    the keystore. WSS4J uses this private
 * key to produce a signature. The signature verification uses the public
 * key to verify the signature.</li>
 * <li><code>KEY_NAME</code> - need the <i>key</i>, not the password,
 * associated with the identifier. WSS4J uses this key to encrypt or
 * decrypt parts of the SOAP request. Note, the key must match the
 * symmetric encryption/decryption algorithm specified (refer to
 * {@link org.apache.ws.security.handler.WSHandlerConstants#ENC_SYM_ALGO}).</li>
 * <li><code>USERNAME_TOKEN_UNKNOWN</code> - either an not specified
 * password type or a password type passwordText. In these both cases <b>only</b>
 * the password variable is <b>set</b>. The callback class now may check if
 * the username and password match. If they don't match the callback class must
 * throw an exception. The exception can be a UnsupportedCallbackException or
 * an IOException.</li>
 * <li><code>SECURITY_CONTEXT_TOKEN</code> - need the key to to be associated
 * with a <code>wsc:SecurityContextToken</code>.</li>
 * </ul>
 *
 * @author Werner Dittmann (Werner.Dittmann@siemens.com).
 */

public class WSPasswordCallback implements Callback {

    public enum Usage {
        UNKNOWN,
        DECRYPT,
        USERNAME_TOKEN,
        SIGNATURE,
        KEY_NAME,
        USERNAME_TOKEN_UNKNOWN,
        SECURITY_CONTEXT_TOKEN,
        CUSTOM_TOKEN,
        ENCRYPTED_KEY_TOKEN,
        SECRET_KEY,
    }

    private String identifier;
    private String password;
    private byte[] key;
    private Usage usage;
    private String type;
    private Element customToken;

    /**
     * Constructor.
     *
     * @param id The application called back must supply the password for
     *           this identifier.
     */
    public WSPasswordCallback(String id, Usage usage) {
        this(id, null, null, usage);
    }

    /**
     * Constructor.
     *
     * @param id The application called back must supply the password for
     *           this identifier.
     */
    public WSPasswordCallback(String id, String pw, String type, Usage usage) {
        identifier = id;
        password = pw;
        this.type = type;
        this.usage = usage;
    }

    /**
     * Get the identifier.
     * <p/>
     *
     * @return The identifier
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Get the identifier.
     * <p/>
     *
     * @return The identifier
     * @deprecated use getIdentifier() instead
     */
    public String getIdentifer() {
        return getIdentifier();
    }

    /**
     * Extended callback interface allows for setting the username as well.
     * Callback functions can change the identifier, this is intended in the usernametoken scenario
     * where the usernametoken denotes the identity, but a fixed identity for signing is used
     * The initial value is that from the configuration file. If this method is not called, the
     * configured identity is used.
     *
     * @param ident The identity.
     */
    public void setIdentifier(String ident) {
        this.identifier = ident;
    }

    /**
     * Set the password.
     * <p/>
     *
     * @param passwd is the password associated to the identifier
     */
    public void setPassword(String passwd) {
        password = passwd;
    }

    /**
     * Get the password.
     * <p/>
     *
     * @return The password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Set the Key.
     * <p/>
     *
     * @param key is the key associated to the identifier
     */
    public void setKey(byte[] key) {
        this.key = key;
    }

    /**
     * Get the key.
     * <p/>
     *
     * @return The key
     */
    public byte[] getKey() {
        return this.key;
    }

    /**
     * Get the usage.
     * <p/>
     *
     * @return The usage for this callback
     */
    public Usage getUsage() {
        return usage;
    }

    /**
     * The password type is only relevant for usage <code>USERNAME_TOKEN</code>
     * and <code>USERNAME_TOKEN_UNKNOWN</code>.
     *
     * @return Returns the passwordType.
     */
    public String getPasswordType() {
        return type;
    }

    /**
     * The key type is only relevant for usage <code>ENCRYPTED_KEY_TOKEN</code>
     *
     * @return Returns the type.
     */
    public String getKeyType() {
        return type;
    }

    public Element getCustomToken() {
        return customToken;
    }

    public void setCustomToken(Element customToken) {
        this.customToken = customToken;
    }
}