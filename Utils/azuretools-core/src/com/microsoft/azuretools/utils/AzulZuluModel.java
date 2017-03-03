/*
 * Copyright (c) Microsoft Corporation
 *   <p/>
 *  All rights reserved.
 *   <p/>
 *  MIT License
 *   <p/>
 *  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 *  documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 *  to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *  <p/>
 *  The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 *  the Software.
 *   <p/>
 *  THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 *  THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 *  TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package com.microsoft.azuretools.utils;

/**
 * Created by vlashch on 1/12/17.
 */
public enum AzulZuluModel {
    OpenJDK_180_u112("Azul Zulu, OpenJDK 1.8.0_u112", "http://azure.azulsystems.com/zulu/zulu8.19.0.1-jdk8.0.112-win_x64.zip", false),
    OpenJDK_180_u92("Azul Zulu, OpenJDK 1.8.0_u92", "http://azure.azulsystems.com/zulu/zulu8.15.0.1-jdk8.0.92-win_x64.zip", false),
    OpenJDK_180_u72("Azul Zulu, OpenJDK 1.8.0_u72", "http://azure.azulsystems.com/zulu/zulu8.13.0.5-jdk8.0.72-win_x64.zip", false),
    OpenJDK_180_u66("Azul Zulu, OpenJDK 1.8.0_u66", "http://azure.azulsystems.com/zulu/zulu1.8.0_66-8.11.0.1-win64.zip", false),
    OpenJDK_180_u60("Azul Zulu, OpenJDK 1.8.0_u60", "http://azure.azulsystems.com/zulu/zulu1.8.0_60-8.9.0.4-win64.zip", false),
    OpenJDK_180_u51("Azul Zulu, OpenJDK 1.8.0_u51", "http://azure.azulsystems.com/zulu/zulu1.8.0_51-8.8.0.3-win64.zip", false),
    OpenJDK_180_u45("Azul Zulu, OpenJDK 1.8.0_u45", "http://azure.azulsystems.com/zulu/zulu1.8.0_45-8.7.0.5-win64.zip", false),
    OpenJDK_180_u40("Azul Zulu, OpenJDK 1.8.0_u40", "http://azure.azulsystems.com/zulu/zulu1.8.0_40-8.6.0.1-win64.zip", false),
    OpenJDK_180_u31("Azul Zulu, OpenJDK 1.8.0_u31", "http://azure.azulsystems.com/zulu/zulu1.8.0_31-8.5.0.1-win64.zip", false),

    OpenJDK_180_u25("Azul Zulu, OpenJDK 1.8.0_u25", "http://azure.azulsystems.com/zulu/zulu1.8.0_25-8.4.0.1-win64.zip", true),
    OpenJDK_180_u20("Azul Zulu, OpenJDK 1.8.0_u20", "http://azure.azulsystems.com/zulu/zulu1.8.0_20-8.3.0.1-win64.zip", true),
    OpenJDK_180_u11("Azul Zulu, OpenJDK 1.8.0_u11", "http://azure.azulsystems.com/zulu/zulu1.8.0_11-8.2.0.1-win64.zip", true),
    OpenJDK_180_u05("Azul Zulu, OpenJDK 1.8.0_u05", "http://azure.azulsystems.com/zulu/zulu1.8.0_05-8.1.0.6-win64.zip", true),

    OpenJDK_170_u121("Azul Zulu, OpenJDK 1.7.0_u121", "http://azure.azulsystems.com/zulu/zulu7.16.0.1-jdk7.0.121-win_x64.zip", false),
    OpenJDK_170_u101("Azul Zulu, OpenJDK 1.7.0_u101", "http://azure.azulsystems.com/zulu/zulu7.14.0.5-jdk7.0.101-win_x64.zip", false),
    OpenJDK_170_u95("Azul Zulu, OpenJDK 1.7.0_u95", "http://azure.azulsystems.com/zulu/zulu7.13.0.1-jdk7.0.95-win_x64.zip", false),
    OpenJDK_170_u91("Azul Zulu, OpenJDK 1.7.0_u91", "http://azure.azulsystems.com/zulu/zulu1.7.0_91-7.12.0.3-win64.zip", false),
    OpenJDK_170_u85("Azul Zulu, OpenJDK 1.7.0_u85", "http://azure.azulsystems.com/zulu/zulu1.7.0_85-7.11.0.3-win64.zip", false),
    OpenJDK_170_u80("Azul Zulu, OpenJDK 1.7.0_u80", "http://azure.azulsystems.com/zulu/zulu1.7.0_80-7.10.0.1-win64.zip", false),
    OpenJDK_170_u76("Azul Zulu, OpenJDK 1.7.0_u76", "http://azure.azulsystems.com/zulu/zulu1.7.0_76-7.8.0.3-win64.zip", false),
    OpenJDK_170_u72("Azul Zulu, OpenJDK 1.7.0_u72", "http://azure.azulsystems.com/zulu/zulu1.7.0_72-7.7.0.1-win64.zip", false),
    OpenJDK_170_u65("Azul Zulu, OpenJDK 1.7.0_u65", "http://azure.azulsystems.com/zulu/zulu1.7.0_65-7.6.0.1-win64.zip", false),

    OpenJDK_170_u55("Azul Zulu, OpenJDK 1.7.0_u55", "http://azure.azulsystems.com/zulu/zulu1.7.0_55-7.4.0.5-win64.zip", true),
    OpenJDK_170_u51("Azul Zulu, OpenJDK 1.7.0_u51", "http://azure.azulsystems.com/zulu/zulu1.7.0_51-7.3.0.4-win64.zip", true),
    OpenJDK_170_u45("Azul Zulu, OpenJDK 1.7.0_u45", "http://azure.azulsystems.com/zulu/zulu1.7.0_45-7.2.1.5-win64.zip", true),
    OpenJDK_170_u40("Azul Zulu, OpenJDK 1.7.0_u40", "http://azure.azulsystems.com/zulu/zulu1.7.0_40-7.1.0.0-win64.zip", true),
    OpenJDK_170_u25("Azul Zulu, OpenJDK 1.7.0_u25", "http://azure.azulsystems.com/zulu/zulu1.7.0_25-7.0.0.0-win64.zip", true),

    OpenJDK_160_u87("Azul Zulu, OpenJDK 1.6.0_u87", "http://azure.azulsystems.com/zulu/zulu6.14.0.1-jdk6.0.87-win_x64.zip", false),
    OpenJDK_160_u79("Azul Zulu, OpenJDK 1.6.0_u79", "http://azure.azulsystems.com/zulu/zulu6.12.0.2-jdk6.0.79-win_x64.zip", false),
    OpenJDK_160_u77("Azul Zulu, OpenJDK 1.6.0_u77", "http://azure.azulsystems.com/zulu/zulu6.11.0.2-jdk6.0.77-win_x64.zip", false),
    OpenJDK_160_u73("Azul Zulu, OpenJDK 1.6.0_u73", "http://azure.azulsystems.com/zulu/zulu1.6.0_73-6.10.0.3-win64.zip", false),
    OpenJDK_160_u69("Azul Zulu, OpenJDK 1.6.0_u69", "http://azure.azulsystems.com/zulu/zulu1.6.0_69-6.9.0.3-win64.zip", false),
    OpenJDK_160_u63("Azul Zulu, OpenJDK 1.6.0_u63", "http://azure.azulsystems.com/zulu/zulu1.6.0_63-6.8.0.1-win64.zip", false),
    OpenJDK_160_u59("Azul Zulu, OpenJDK 1.6.0_u59", "http://azure.azulsystems.com/zulu/zulu1.6.0_59-6.7.0.2-win64.zip", false),
    OpenJDK_160_u56("Azul Zulu, OpenJDK 1.6.0_u56", "http://azure.azulsystems.com/zulu/zulu1.6.0_56-6.6.0.1-win64.zip", false),

    OpenJDK_160_u53("Azul Zulu, OpenJDK 1.6.0_u53", "http://azure.azulsystems.com/zulu/zulu1.6.0_53-6.5.0.2-win64.zip", true),
    OpenJDK_160_u49("Azul Zulu, OpenJDK 1.6.0_u49", "http://azure.azulsystems.com/zulu/zulu1.6.0_49-6.4.0.6-win64.zip", true),
    OpenJDK_160_u47("Azul Zulu, OpenJDK 1.6.0_u47", "http://azure.azulsystems.com/zulu/zulu1.6.0_47-6.3.0.3-win64.zip", true);

    private String name;
    private static String licenseUrl = "http://openjdk.java.net/legal/gplv2+ce.html";
    private String downloadUrl;
    private boolean isDeprecated = false;

    AzulZuluModel(String name, String downloadUrl, boolean isDeprecated) {
        this.name = name;
        this.downloadUrl = downloadUrl;
        this.isDeprecated = isDeprecated;
    }

    public String getName() {
        return name;
    }

    public static String getLicenseUrl() {
        return licenseUrl;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public boolean isDeprecated() {
        return isDeprecated;
    }

    @Override
    public String toString() {
        return name;
    }
}
