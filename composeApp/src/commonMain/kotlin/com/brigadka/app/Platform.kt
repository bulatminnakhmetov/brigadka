package com.brigadka.app

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform