# java-Dependency-tree

A POJO JAVA dependency tree (Dependency.java) and dependency collection (DependencyForest.java) providing find, '
multi-root' dependency and intelligent to/from JSON conversion functionality.

There are plenty of nested structures out there but never quite the one you want. This is a Dependency tree with the
following features:

0. POJO. It has no reliance on UI or persistence APIs.
1. It validates dependencies on addition intelligently and kicks out circular dependencies.
2. It maintains a bi-directional relationship of dependencies and dependants so the user can choose which direction (
   leaves in or roots out) to use the tree as best fits their data and scenario.
3. Serializing to JSON is smart, specifically it:
   a. Does not overflow the recursion stack when ObjectMapping each Dependency and its dependants / dependencies. This
   is achieved by setting the direction of recursion, guaranteeing all subordinate objects honour that direction when
   serializing and only recursing in that direction.
   b. either serialize as a JSON tree with multiple roots where the roots were those tasks with no dependants (the ultimate ancestors of all dependencies) or serialize as a tree with multiple roots where the roots were those tasks that were not dependants of anything (the outermost leaves of the dependency tree)
4. A leaf node may depend on any other node (save for circular dependencies). Note that this makes it not really a tree
   because the outermost leaves can have as their dependencies ancestors with different roots - like a leaf growing off
   two different trees.
5. Dependencies can be intelligently removed from the tree. If a leaf node is removed, it is simply 'pruned'. If a root
   node is removed, its child dependants become root nodes. If a branch node is removed, its orphaned children become
   children of its parents and its non-orphaned children are left with only their extant parents.
6. A List of routes to root nodes from all leaf objects is maintained in each Dependency instance. When accessed, this
   List is sorted with longest routes first and shortest routes last.

So this is the beginning. It works as is and over time, I'll add iterable and search functionalitiy.

TODO: Improve JavaDoc

TODO: Implement standard interfaces (decide between Collection, List, Map or all / other) and provide iterable and other
standard methods.

TODO: Add getRootNodes() conveience method to Dependency.java.



