# java-Dependency-tree
A POJO JAVA dependency tree (Dependency.java) and dependency collection (DependencyForest.java) providing iterator, find and 'multiple root' dependency and intelligent to/from JSON conversion functionality

There are plenty of nested structures out there but never quite the one you want. In this case, I needed a dependency tree with a few quirks.

1. I wanted it to validate dependencies on addition intelligently and kick out circular dependencies.
2. I wanted it to maintain a bi-directional relationship of dependencies and dependants.
3. I wanted to be smart when serializing to JSON, specifically that it should:
   a. not blow the recursion stack when ObjectMapper-izing.
   b. either serialize as a JSON tree with multiple roots where the roots were those tasks with no dependants (the ultimate ancestors of all dependencies) or serialize as a tree with multiple roots where the roots were those tasks that were not dependants of anything (the outermost leaves of the dependency tree)
4. I wanted it to be possible for a leaf node to depend on any other node (save for circular dependencies). Note that this makes it not really a tree because the outermost leaves can have as their dependencies ancestors with completely different roots - like a leaf growing off two different trees.

So this is the beginning. It works as is and over time, I'll add iterable and search functionalitiy.

TODO: JavaDoc
TODO: Search and iterable - proper implementation of some Collection, havne't decided which yet.



