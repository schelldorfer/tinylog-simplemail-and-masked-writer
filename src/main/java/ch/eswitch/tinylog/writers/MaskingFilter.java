package ch.eswitch.tinylog.writers;

/**
 * Contains all information of a masking filter
 */
class MaskingFilter
{
    /**
     * prefix search string
     */
    public final String prefix;
    /**
     * suffix search string
     */
    public final String suffix;
    /**
     * number of characters used for fixed length masking
     */
    public final int fixedLength;

    /**
     *
     * @param prefix prefix
     * @param suffix suffix
     * @param fixedLength number of characters used for fixed length masking
     */
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
