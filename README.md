[![Release](https://jitpack.io/v/com.github.oriley-me/crate.svg)](https://jitpack.io/#com.github.oriley-me/crate) [![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0) [![Build Status](https://travis-ci.org/oriley-me/crate.svg?branch=master)](https://travis-ci.org/oriley-me/crate) [![Dependency Status](https://www.versioneye.com/user/projects/56e39ab7df573d00472cd399/badge.svg?style=flat)](https://www.versioneye.com/user/projects/56e39ab7df573d00472cd399)

# Crate

Crate is a simple gradle plugin to generate a list static classes for all files/folders included in your projects
assets directory, for compile time safety.

To use:

```java
Crate crate = new Crate(context);

try {
    InputStream is = crate.get(Crate.fonts.ROBOTO_LIGHT_TTF);
    // Do what you will with the input stream
} catch (IOException e) {
    // Handle exception
}

crate.close();
```

No more string literals or typos, all your assets can be accessed with confidence!
You can also retrieve a list of all files in a root directory via:

```java
for (Crate.Asset asset : Crate.fonts.LIST) {
    // Perform action
}
```

Each `Asset` has two methods, `asset.getPath()` will return the full path as required by an `AssetManager`, and
`asset.getName()`, which will return the filename only. If a file happens to be a TTF/OTF font, the `FontAsset` class
will be used, which has an extra `fontAsset.getFontName()` method for convenience.

If all files in a folder are font files, the static `LIST` will be of type `List<FontAsset>`, otherwise the generic
`List<Asset>` type will be used.

# Gradle Dependency

1. Add JitPack.io to your repositories list in the root projects build.gradle:

```gradle
repositories {
    maven { url "https://jitpack.io" }
}
```

2. Add crate-plugin to your buildscript classpath:

```gradle
buildscript {
    dependencies {
        classpath 'me.oriley:crate:0.1'
    }
}
```

3. Apply plugin to your application or library project:

```gradle
apply plugin: 'com.android.application' || apply plugin: 'com.android.library'
apply plugin: 'me.oriley.crate-plugin'
```