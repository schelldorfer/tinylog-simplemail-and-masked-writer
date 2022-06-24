package com.github.schelldorfer.tinylog.writers;

class MaskingFilter
{
    public final String prefix;
    public final String suffix;
    public final int fixedLength;

    MaskingFilter(String prefix, String suffix, String fixedLength)
    {
        this.prefix = prefix.trim();
        this.suffix = (suffix == null || suffix.length() == 0) ? null : suffix.trim();

        int fixedLengthParsed = -1;
        if (fixedLength != null && fixedLength.length() > 0)
        {
            try
            {
                fixedLengthParsed = Integer.parseInt(fixedLength.trim());
            } catch (NumberFormatException e)
            {
            }
        }

        this.fixedLength = fixedLengthParsed;
    }
}
