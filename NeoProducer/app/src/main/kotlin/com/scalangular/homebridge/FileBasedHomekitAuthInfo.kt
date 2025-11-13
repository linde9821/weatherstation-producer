package com.scalangular.homebridge

import io.github.hapjava.server.HomekitAuthInfo
import io.github.hapjava.server.impl.HomekitServer
import java.io.File
import java.math.BigInteger
import java.util.Base64
import java.util.Properties

class FileBasedHomekitAuthInfo(
    private val storageFile: File,
    private val pin: String,
    mac: String? = null,
    salt: BigInteger? = null,
    privateKey: ByteArray? = null
) : HomekitAuthInfo {

    private val props = Properties()
    private val usersFile = File(storageFile.parentFile, "users.properties")
    private val userProps = Properties()

    private val _mac: String
    private val _salt: BigInteger
    private val _privateKey: ByteArray

    init {
        // Load or create storage
        if (storageFile.exists()) {
            storageFile.inputStream().use { props.load(it) }
        }

        // Load or create users
        if (usersFile.exists()) {
            usersFile.inputStream().use { userProps.load(it) }
        }

        // Initialize or load MAC
        _mac = mac ?: props.getProperty("mac") ?: HomekitServer.generateMac().also {
            props.setProperty("mac", it)
            save()
        }

        // Initialize or load Salt
        _salt = salt ?: props.getProperty("salt")?.let {
            BigInteger(it, 16)
        } ?: HomekitServer.generateSalt().also {
            props.setProperty("salt", it.toString(16))
            save()
        }

        // Initialize or load Private Key
        _privateKey = privateKey ?: props.getProperty("privateKey")?.let {
            Base64.getDecoder().decode(it)
        } ?: HomekitServer.generateKey().also {
            props.setProperty("privateKey", Base64.getEncoder().encodeToString(it))
            save()
        }
    }

    override fun getPin(): String = pin

    override fun getMac(): String = _mac

    override fun getSalt(): BigInteger = _salt

    override fun getPrivateKey(): ByteArray = _privateKey

    override fun createUser(username: String, publicKey: ByteArray, isAdmin: Boolean) {
        val encodedKey = Base64.getEncoder().encodeToString(publicKey)
        userProps.setProperty("$username.key", encodedKey)
        userProps.setProperty("$username.admin", isAdmin.toString())
        saveUsers()
    }

    override fun removeUser(username: String) {
        userProps.remove("$username.key")
        userProps.remove("$username.admin")
        saveUsers()
    }

    override fun getUserPublicKey(username: String): ByteArray? {
        return userProps.getProperty("$username.key")?.let {
            Base64.getDecoder().decode(it)
        }
    }

    override fun userIsAdmin(username: String): Boolean {
        return userProps.getProperty("$username.admin")?.toBoolean() ?: true
    }

    override fun listUsers(): Collection<String> {
        return userProps.stringPropertyNames()
            .filter { it.endsWith(".key") }
            .map { it.removeSuffix(".key") }
    }

    override fun hasUser(): Boolean {
        return userProps.stringPropertyNames().any { it.endsWith(".key") }
    }

    private fun save() {
        storageFile.outputStream().use { props.store(it, "HomeKit Auth Info") }
    }

    private fun saveUsers() {
        usersFile.outputStream().use { userProps.store(it, "HomeKit Users") }
    }
}