package com.github.schelldorfer.tinylog.writers;

class Filter {
    public final String prefix;
    public final String suffix;

    Filter(String prefix, String suffix) {
        this.prefix = prefix;
        this.suffix = suffix;
    }
}
