# java-Dependency-tree
A POJO JAVA dependency tree collection providing iterator, find and 'multiple root' dependency and intelligent to/from JSON conversion functionality

There are plenty of nested structures out there but never quite the one you want. In this case, I needed a dependency tree with a few quirks.

1. I wanted it to validate dependencies on addition and kick out circular dependencies.
2. I wanted it to maintain a bi-directional relationship of dependencies and dependants.
3. I wanted to be smart when serializing to JSON, specifically that it should:
   a. not blow the recursion stack when ObjectMapper-izing.
   b. either serialize as a JSON tree with multiple roots where the roots were those tasks with no dependants (the ultimate ancestors of all dependencies) or serialize         as a tree with multiple roots where the roots were those tasks that were not dependants of anything (the outermost leaves of the dependency tree)
4. I wanted it to be possible for a leaf node to depend on any other node (save for circular dependencies). Not that this makes it not really a tree because the outermost leaves can have as their dependencies ancestors with completely different roots - like leaf growing off two different trees.

So this is the beginning. It works as is and over time, I'll add iterable and search functionalitiy. I may also refactor and create a DependencyForest class to allow for multiple sets of root nodes in the same JVM. At present, the Dependency uses static members and methods to maintain lists of the dependant and dependency root nodes so you can only have one forest in your JVM at any one time.

No JAVADOC yet.



