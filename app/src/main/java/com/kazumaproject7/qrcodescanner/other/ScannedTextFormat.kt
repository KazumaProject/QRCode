package com.kazumaproject7.qrcodescanner.other

import android.content.Intent
import android.net.Uri

fun String.getEmailEmailTypeOne(): String {
    val str = this.split(":" ).toTypedArray()
    return if (str.size == 5){
        str[2].replace(";SUB","")
    } else{
        ""
    }
}

fun String.getSubjectEmailTypeOne(): String {
    val str = this.split(":" ).toTypedArray()
    return if (str.size == 5){
        str[3].replace(";BODY","")
    } else{
        ""
    }
}

fun String.getMessageEmailTypeOne(): String {
    val str = this.split(":" ).toTypedArray()
    return if (str.size == 5){
        str[4].replace(";;","")
    } else{
        ""
    }
}

fun String.getEmailEmailTypeTwo():String{
    if (this.contains("?body=") || this.contains("&subject=")){
        when{
            this.contains("?body=") && !this.contains("&subject=") ->{
                val str = this.split("?" ).toTypedArray()
                return if (str.size >=2) {
                    str[0].replace("mailto:", "")
                } else{
                    ""
                }
            }
            !this.contains("?body=") && this.contains("&subject=") ->{
                return ""
            }
            this.contains("?body=") && this.contains("&subject=") ->{
                val str = this.split("?" ).toTypedArray()
                return if (str.size >=2){
                    val str1 = str[0].replace("mailto:","")
                    str1
                } else {
                    ""
                }
            }
            else ->{
                return ""
            }
        }
    }else{
        if (this.contains("mailto:")){
            return when{
                this.contains("?subject=") ->{
                    val emailStr = this.replace("mailto:","")
                    val str = emailStr.split("?" ).toTypedArray()
                    if (str.size >= 2) {
                        str[0]
                    } else {
                        ""
                    }
                }
                else -> {
                    replace("mailto:", "")
                }
            }
        } else if (this.contains("MAILTO")) {
            val emailStr = this.replace("MAILTO","")
            return emailStr.replace(":","").replace(" ","")
        } else {
            return ""
        }
    }
}

fun String.getEmailSubjectTypeTwo():String{
    if (this.contains("?body=") || this.contains("&subject=")){
        when{
            this.contains("?body=") && !this.contains("&subject=") ->{
                return ""
            }
            !this.contains("?body=") && this.contains("&subject=") ->{
                return ""
            }
            this.contains("?body=") && this.contains("&subject=") ->{
                val str = this.split("?" ).toTypedArray()
                return if (str.size >=2){
                    val str2 = str[1].split("&").toTypedArray()
                    str2[1].replace("subject=","")
                } else {
                    ""
                }
            }
            else ->{
                return ""
            }
        }
    }else{
        if (this.contains("mailto:")){
            return when{
                this.contains("?subject=") ->{
                    val emailStr = this.replace("mailto:","")
                    val str = emailStr.split("?" ).toTypedArray()
                    if (str.size >= 2) {
                        str[1].replace("subject=","")
                    } else {
                        ""
                    }
                }
                else -> {
                    ""
                }
            }
        } else if (this.contains("MAILTO")) {
            return ""
        } else {
            return ""
        }
    }
}

fun String.getEmailMessageTypeTwo():String{
    if (this.contains("?body=") || this.contains("&subject=")){
        when{
            this.contains("?body=") && !this.contains("&subject=") ->{
                val str = this.split("?" ).toTypedArray()
                return if (str.size == 2){
                    str[1].replace("body=","")
                }else {
                    ""
                }
            }
            !this.contains("?body=") && this.contains("&subject=") ->{
                return ""
            }
            this.contains("?body=") && this.contains("&subject=") ->{
                val str = this.split("?" ).toTypedArray()
                return if (str.size >=2){
                    val str2 = str[1].split("&").toTypedArray()
                    str2[0].replace("body=","")
                } else {
                    ""
                }
            }
            else ->{
                return ""
            }
        }
    }else{
        return if (this.contains("mailto:")){
            when{
                this.contains("?subject=") ->{
                    ""
                }
                else -> {
                    ""
                }
            }
        } else if (this.contains("MAILTO")) {
            ""
        } else {
            ""
        }
    }
}

fun String.getSMSNumber():String{
    val str = this.split(":" ).toTypedArray()
    return when(str.size){
        2 ->{
            ""
        }
        3 ->{
            str[1]
        }
        else ->{
            ""
        }
    }
}

fun String.getSMSMessage():String{
    val str = this.split(":" ).toTypedArray()
    return when(str.size){
        2 ->{
            ""
        }
        3 ->{
            str[2]
        }
        else ->{
            ""
        }
    }
}

fun String.getWifiSSID():String{
    val str = this.split(":" ).toTypedArray()
    return when(str.size){
        6 ->{
            str[3].replace(";P","")
        }
        else ->{
            ""
        }
    }
}

fun String.getWifiPassword():String{
    val str = this.split(":" ).toTypedArray()
    return when(str.size){
        6 ->{
            str[4].replace(";H","")
        }
        else ->{
            ""
        }
    }
}

fun String.getWifiEncryptionType():String{
    val str = this.split(":" ).toTypedArray()
    return when(str.size){
        6 ->{
            str[2].replace(";S","")
        }
        else ->{
            ""
        }
    }
}

fun String.getWifiIsHidden():String{
    val str = this.split(":" ).toTypedArray()
    return when(str.size){
        6 ->{
            if (str[5].replace(";","") == ""){
                "false"
            } else {
                str[5].replace(";","")
            }
        }
        else ->{
            ""
        }
    }
}

fun String.getCryptocurrencyType():String {
    val str = this.split(":" ).toTypedArray()
    return when(str.size){
        2 ->{
            str[0]
        }
        else ->{
            ""
        }
    }
}

fun String.getCryptocurrencyAddress():String {
    val str = this.split(":" ).toTypedArray()
    when(str.size){
        2 ->{
            val str2 = str[1].split("?" ).toTypedArray()
            return when(str2.size){
                2 ->{
                    str2[0]
                }
                else ->{
                    ""
                }
            }
        }
        else ->{
            return ""
        }
    }
}

fun String.getCryptocurrencyAmount():String {
    val str = this.split(":" ).toTypedArray()
    when(str.size){
        2 ->{
            val str2 = str[1].split("?" ).toTypedArray()
            when(str2.size){
                2 ->{
                    val str3 = str2[1].split("&" ).toTypedArray()
                    return when(str3.size){
                        2 ->{
                            str3[0].replace("amount=","")
                        }
                        else ->{
                            ""
                        }
                    }
                }
                else ->{
                    return ""
                }
            }
        }
        else ->{
            return ""
        }
    }
}

fun String.getCryptocurrencyMessage():String {
    val str = this.split(":" ).toTypedArray()
    when(str.size){
        2 ->{
            val str2 = str[1].split("?" ).toTypedArray()
            when(str2.size){
                2 ->{
                    val str3 = str2[1].split("&" ).toTypedArray()
                    return when(str3.size){
                        2 ->{
                            str3[1].replace("message=","")
                        }
                        else ->{
                            ""
                        }
                    }
                }
                else ->{
                    return ""
                }
            }
        }
        else ->{
            return ""
        }
    }
}
