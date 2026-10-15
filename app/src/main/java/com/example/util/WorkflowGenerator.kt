package com.example.util

object WorkflowGenerator {

    const val DEFAULT_WORKFLOW_PATH = ".github/workflows/build-apk.yml"

    fun generateAndroidBuildWorkflow(): String {
        return """
name: Android APK Builder (GitBuilder)

on:
  push:
    branches: [ main, master ]
  workflow_dispatch:

concurrency:
  group: gitbuilder-${'$'}{{ github.ref }}
  cancel-in-progress: true

jobs:
  build:
    name: Compile Android APK
    runs-on: ubuntu-latest
    timeout-minutes: 30

    steps:
      - name: Checkout Source Code
        uses: actions/checkout@v4

      - name: Setup JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'
          cache: 'gradle'

      - name: Prepare Gradle Environment
        run: |
          echo "Verificando gradlew e permissões..."
          if [ ! -f "gradlew" ]; then
            echo "Aviso: 'gradlew' não encontrado na raiz. Gerando wrapper..."
            gradle wrapper --gradle-version 8.9 || gradle wrapper
          fi
          chmod +x gradlew
          ls -la

      - name: Compile Debug APK
        run: |
          echo "Iniciando compilação do APK com assembleDebug..."
          ./gradlew assembleDebug --stacktrace --no-daemon

      - name: Inspect Generated APKs
        if: always()
        run: |
          echo "Localizando arquivos APK gerados:"
          find . -name "*.apk" -ls || true

      - name: Upload APK Artifact
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: app-debug-apk
          path: |
            **/build/outputs/apk/**/*.apk
          if-no-files-found: warn
          retention-days: 7
""".trimIndent()
    }
}
