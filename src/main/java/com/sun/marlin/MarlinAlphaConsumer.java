package com.sun.marlin;

import com.sun.openpisces.AlphaConsumer;

public interface MarlinAlphaConsumer extends AlphaConsumer {

    public void clearAlphas(final int pix_y);

    public void setAndClearRelativeAlphas(int[] blkFlags, int alphaDeltas[], int pix_y,
                                          int firstdelta, int lastdelta);
}
