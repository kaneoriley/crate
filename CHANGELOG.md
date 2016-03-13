Change Log
==========

## Version 0.2 WIP

 *  Improvement: Add debug logging for testing
 *  Improvement: Use TreeMap to keep LIST fields sorted by field name
 *  Feature: Add FULL_LIST field containing all assets for project
 *  Improvement: Make list fields unmodifiable
 *  Feature: Add Gradle DSL extension for toggling debug logging and setting output package/class name
 *  Feature: Add option to disable static mode, to reduce ongoing memory requirements and simplify the API
 *  Fix: Don't close AssetManager as it can cause issues
 *  Feature: Add typeface retrieval method and caching for font assets


## Version 0.1.1

_2016-3-13_

 *  Feature: Add special handling for font assets
 *  Fix: Validate Crate JAR hash and force mergeAssets task to run if changed
 *  Fix: Better field name sanitising (shouldn't be able to choke on invalid filenames anymore)
 *  Improvement: Migrate java class generation to JavaPoet


## Version 0.1

_2016-03-12_

 *  Initial release.
