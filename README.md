[![Release](https://jitpack.io/v/com.github.oriley-me/crate.svg)](https://jitpack.io/#com.github.oriley-me/crate) [![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0) [![Build Status](https://travis-ci.org/oriley-me/crate.svg?branch=master)](https://travis-ci.org/oriley-me/crate) [![Dependency Status](https://www.versioneye.com/user/projects/56e39ab7df573d00472cd399/badge.svg?style=flat)](https://www.versioneye.com/user/projects/56e39ab7df573d00472cd399)

# Crate

Crate is a simple gradle plugin to generate a list (static by default) of classes for all files/folders included in your projects
assets directory, for compile time safety. This is similar to how the `R` classes for resources work. It also has the
advantage of removing the need to use the notoriously slow `AssetManager.list()` methods.

Also included is a built-in typeface cache to ensure you don't allocate multiple blocks of memory for the same font.

No more string literals or typos, all your assets can be accessed with confidence!

Each `Asset` has two methods, `asset.getPath()` will return the full path as required by an `AssetManager`, and
`asset.getName()`, which will return the filename only. If a file happens to be a TTF/OTF font, the `FontAsset` class
will be used, which has an extra `fontAsset.getFontName()` method for convenience.

To use:

```java
Crate crate = new Crate(context);

// Usage for InputStream
try {
    InputStream is = crate.open(Crate.assets.svgs.AWESOME_BACKGROUND_SVG);
    // Do what you will with the input stream
} catch (IOException e) {
    // Handle exception
}

// Usage for Typeface
Typeface typeface = crate.getTypeface(Crate.assets.fonts.ROBOTO_SLAB_TTF);
```

If you set `staticMode` to false (read below for an explanation), you must use the `Crate` instance to access assets.
In this situation, I'd strongly suggest storing the `Crate` in a member variable to reduce workload of instantiating,
and remember to `null` the field when you are done, to ensure it can be garbage collected.

In this mode, each assets has extra methods to directly open the `InputStream`/`Typeface`, as demonstrated below:
```java
// In constructor
mCrate = new Crate(context);

// Usage for InputStream
try {
    InputStream is = mCrate.assets.svgs.AWESOME_BACKGROUND_SVG.open();
    // Do what you will with the input stream
} catch (IOException e) {
    // Handle exception
}

// Usage for Typeface - FontAssets only
Typeface typeface = mCrate.assets.fonts.ROBOTO_SLAB_TTF.getTypeface();

// When no longer required
mCrate = null;
```

You can also retrieve a list of all files in a root directory via:
```java
for (Crate.Asset asset : Crate.assets.fonts.LIST) {
    // Perform action
}
```
Or, for all assets in your project:
```java
for (Crate.Asset asset : Crate.FULL_LIST) {
    // Perform action
}
```

If all files in a folder are font files, the `LIST` will be of type `List<FontAsset>`, otherwise the generic
`List<Asset>` type will be used.

## Gradle Dependency

 * Add JitPack.io to your repositories list in the root projects build.gradle:

```gradle
repositories {
    maven { url "https://jitpack.io" }
}
```

 * Add crate-plugin to your buildscript classpath:

```gradle
buildscript {
    dependencies {
        classpath 'me.oriley:crate:0.2'
    }
}
```

If you would like to run a newer version you can change the version number to `-SNAPSHOT` for the latest master
branch, or a specific commit hash if you need an exact version. That's the beauty of JitPack. Just beware that API's
can be subject to change without notice if you don't stick to a release version.

 * Apply the plugin to your application or library project:

```gradle
apply plugin: 'com.android.application' || apply plugin: 'com.android.library'
apply plugin: 'me.oriley.crate-plugin'
```

Crate also includes a Gradle DSL extension to provide some optional settings. Declare it in your project build.gradle as follows:
```groovy
crate {
    // Default is false, will output more info to gradle log if set to true
    debugLogging = true

    // Default is true, setting to false will mean all values can be freed up by the VM when memory is required, at
    // the cost of requiring an instance of Crate to access values. I would suggest setting this to false if you have
    // a LOT of assets, or you like the convenience of the delegated open methods.
    staticMode = false

    // By default the package name is read from the application manifest. If that fails
    // for some reason, or you would like the Crate class to be generated elsewhere,
    // set the package name here
    packageName = "my.package.name"

    // If you'd rather the class be name something other than 'Crate', set this property
    className = "NotACrate"
}
```

Now just perform a gradle sync and you're done. You can now have compile time safety with all your projects assets.

## Example

As an example of how efficient and useful `Crate` can be, here's all the code you will need to display an entire folder
of fonts in a `RecyclerView`, with full `Typeface` caching and font display names automatically retrieved and set.

```java
class FontViewHolder extends RecyclerView.ViewHolder {

    TextView textView;

    FontViewHolder(View view) {
        super(view);
        textView = (TextView) view.findViewById(R.id.text_view);
    }
}

class FontRecyclerAdapter extends RecyclerView.Adapter<FontViewHolder> {

    private final Crate mCrate;

    FontRecyclerAdapter(Context context) {
        mCrate = new Crate(context);
    }

    @Override
    public FontViewHolder onCreateViewHolder(ViewGroup viewGroup, int position) {
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.font_view_item, viewGroup, false);
        return new FontViewHolder(view);
    }

    @Override
    public void onBindViewHolder(FontViewHolder holder, int position) {
        FontAsset fontAsset = mCrate.assets.fonts.LIST.get(position);
        holder.textView.setText(fontAsset.getFontName());
        holder.textView.setTypeface(fontAsset.getTypeface());
    }

    @Override
    public int getItemCount() {
        return mCrate.assets.fonts.LIST.size();
    }
}

```

Just add that to any `Activity` hosting a `RecyclerView`, create and set the `FontRecyclerAdapter` and viola:

![Demo](https://raw.githubusercontent.com/oriley-me/crate/master/crate-demo.png)