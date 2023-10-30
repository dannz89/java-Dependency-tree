package com.ddt.dependencyutils.exception;

import com.ddt.dependencyutils.Dependency;

public class CircularDependencyException extends Exception {
    private Dependency dependency;

    public CircularDependencyException(Dependency dependency) {
        super();
        this.dependency = dependency;
    }

    @Override
    public String getMessage() {
        return "["+this.dependency.toString()+"] cannot depend on itself. " +super.getMessage();
    }
}
