package com.example.myfirstapp.backend.protocolDriver;

public class checksumCalculator {
    protected static short CRCCompVal = -32768;

    protected static short CRCGenPoly = 4129;

    protected static short CRCStart = -1;

    protected static short[] gap_crc_tab = new short[256];

    static  {
        short c;
        for (int j=0; j<=255; j++)
        {
            c = (short)(j<<8);
            for (int k=0;k<8;k++)
            {
                if ((c&CRCCompVal)!=0) {
                    c = (short) ((c<<=1)^CRCGenPoly);
                }
                else {
                    c<<=1;
                }
            }
            gap_crc_tab[j]=c;
        }
    }

    public static short gap_crc(byte[] paramArrayOfByte, int paramInt1, int paramInt2)
    {
        return gap_crc(paramArrayOfByte, paramInt1, paramInt2, CRCStart);
    }

    public static short gap_crc(byte[] paramArrayOfByte, int paramInt1, int paramInt2, short paramShort) {
        int i;
        for (i = paramInt1; i < paramInt1 + paramInt2; i++)
            paramShort = (short)((paramShort & 0xFF) << 8 ^ gap_crc_tab[(paramArrayOfByte[i] ^ paramShort >> 8 & 0xFF) & 0xFF]);
        return paramShort;
    }
}


