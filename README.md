# 色えんぴつ
お絵かき帳（`Lambda + S3 + CloudFront`）のクライアントです。  
受け付け用`S3 バケット`に投稿して、配信用`S3 バケット`の中にある写真を表示します。

自分だけが使うアプリ。
`Kotlin Multiplatform`で、`Android`と`Web ブラウザ`で動くはずです。

# 環境構築
- このリポジトリを`clone`して`Android Studio`で開く
- しばらく待つ
- Android の場合
  - 端末を繋いで実行ボタンを押す
- Web の場合
  - `Gradle`のコマンド入力で
    - `gradle :composeApp:wasmJsBrowserDevelopmentRun`
  - ターミナルで
    - `gradlew :composeApp:wasmJsBrowserDevelopmentRun`

# 本番更新
`GitHub Actions`書いてないので、自分でビルドして、Webブラウザ版なら`S3`や`静的サイトホスティングサービス`にアップロードしてください。  

`Android`は`APK/AAB`を作る手順で。

`web ブラウザ版`は`gradle :composeApp:wasmJsBrowserDistribution`を叩いてください。  
`composeApp/build/dist/wasmJs/productionExecutable`の中身を静的サイト公開すればよいです。

--- default README ---

This is a Kotlin Multiplatform project targeting Android, Web.

* `/composeApp` is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - `commonMain` is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    `iosMain` would be the right folder for such calls.


Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html),
[Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/#compose-multiplatform),
[Kotlin/Wasm](https://kotl.in/wasm/)…

We would appreciate your feedback on Compose/Web and Kotlin/Wasm in the public Slack channel [#compose-web](https://slack-chats.kotlinlang.org/c/compose-web).
If you face any issues, please report them on [GitHub](https://github.com/JetBrains/compose-multiplatform/issues).

You can open the web application by running the `:composeApp:wasmJsBrowserDevelopmentRun` Gradle task.