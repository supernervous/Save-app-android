package io.scal.secureshareui.controller

interface RequestListener {
    fun transferred(bytes: Long)
    fun continueUpload(): Boolean
    fun transferComplete()
}