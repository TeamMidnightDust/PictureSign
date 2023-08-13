/* Jheora
 * Copyright (C) 2004 Fluendo S.L.
 *  
 * Written by: 2004 Wim Taymans <wim@fluendo.com>
 *             2008 Maik Merten <maikmerten@googlemail.com>
 *   
 * Many thanks to 
 *   The Xiph.Org Foundation http://www.xiph.org/
 * Jheora was based on their Theora reference decoder.
 *   
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public License
 * as published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Library General Public License for more details.
 * 
 * You should have received a copy of the GNU Library General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package eu.midnightdust.picturesign.ogv.jheora;

import eu.midnightdust.picturesign.ogv.jogg.*;

public class Quant 
{ 
    
  private static int ilog (long v)
  {
    int ret=0;

    while (v != 0) {
      ret++;
      v>>=1;
    }
    return ret;
  }

  public static int readQTables(TheoraInfo ci, OggBuffer opb) {
    /* Variable names according to Theora spec where it makes sense.
     * I *know* this may violate Java coding style rules, but I consider
     * readability against the Theora spec to be more important */
      
    long NBITS,value;
    int x, bmi, NBMS;

    /* A 2 × 3 array containing the number of quant ranges for a
       given qti and pli , respectively. This is at most 63. */
    int[][] NQRS = new int[2][3];
    
    /* A 2 × 3 × 63 array of the sizes of each quant range for a
       given qti and pli , respectively. Only the first NQRS[qti ][pli ]
       values are used. */
    int[][][] QRSIZES = new int[2][3][63];
    
    /* A 2 × 3 × 64 array of the bmi ’s used for each quant
       range for a given qti and pli, respectively. Only the first
       (NQRS[qti ][pli ] + 1) values are used. */
    int[][][] QRBMIS = new int[2][3][64];
    
    int qri, qi, qtj, plj;
    
    
    /* 1. Read a 4-bit unsigned integer. Assign NBITS the value read, plus one. */
    NBITS = opb.readB(4); NBITS++;
    
    /* 2. For each consecutive value of qi from 0 to 63, inclusive:
          (a) Read an NBITS-bit unsigned integer as ACSCALE[qi ]. */
    for(x=0; x < 64; x++) {
      value = opb.readB((int)NBITS);
      if(NBITS<0) return Result.BADHEADER;
      ci.AcScaleFactorTable[x]=(int)value;
    }
    
    /* 3. Read a 4-bit unsigned integer. Assign NBITS the value read, plus one. */
    NBITS = opb.readB(4); NBITS++;
    
    /* 4. For each consecutive value of qi from 0 to 63, inclusive:
          (a) Read an NBITS-bit unsigned integer as DCSCALE[qi ]. */
    for(x=0; x<Constants.Q_TABLE_SIZE; x++) {
      value = opb.readB((int)NBITS);
      if(NBITS<0) return Result.BADHEADER;
      ci.DcScaleFactorTable[x]=(short)value;
    }
    
    /* 5. Read a 9-bit unsigned integer. Assign NBMS the value decoded, plus
       one. NBMS MUST be no greater than 384. */
    NBMS = opb.readB(9); NBMS++;
    if(NBMS > 384) {
      return Result.BADHEADER;
    }
    ci.MaxQMatrixIndex = NBMS;
    
    /* 6. For each consecutive value of bmi from 0 to (NBMS - 1), inclusive:
         (a) For each consecutive value of ci from 0 to 63, inclusive:
             i. Read an 8-bit unsigned integer as BMS[bmi ][ci ]. */
    
    ci.qmats= new short[NBMS*64];
    for(bmi=0; bmi<NBMS; bmi++) {
      for(x=0; x<64; x++) {
        value = opb.readB(8);
        if(NBITS<0)return Result.BADHEADER;
        ci.qmats[(bmi<<6)+x]=(short)value;
      }
    }
    
    /* 7. For each consecutive value of qti from 0 to 1, inclusive: */
    for(int qti = 0; qti <= 1; ++qti) {
      /* (a) For each consecutive value of pli from 0 to 2, inclusive: */
      for(int pli = 0; pli <= 2; ++pli) {
        int NEWQR;
        if(qti > 0 || pli > 0) {
          /* i. If qti > 0 or pli > 0, read a 1-bit unsigned integer as NEWQR. */
          NEWQR = opb.readB(1);
        } else {
          /* ii. Else, assign NEWQR the value one. */
          NEWQR = 1;
        }
            
        if(NEWQR == 0) {
          /* If NEWQR is zero, then we are copying a previously defined set
             of quant ranges. In that case: */ 
                
          int RPQR;
          if(qti > 0) {
            /* A. If qti > 0, read a 1-bit unsigned integer as RPQR. */
            RPQR = opb.readB(1);
          } else {
            /* B. Else, assign RPQR the value zero. */
            RPQR = 0;
          }
                
          if(RPQR == 1) {
            /* C. If RPQR is one, assign qtj the value (qti - 1) and assign plj
               the value pli . This selects the set of quant ranges defined
               for the same color plane as this one, but for the previous
               quantization type. */
            qtj = qti - 1;
            plj = pli;
          } else {
            /* D. Else assign qtj the value (3 * qti + pli - 1)//3 and assign plj
               the value (pli + 2)%3. This selects the most recent set of
               quant ranges defined. */
            qtj = (3 * qti + pli - 1) / 3;
            plj = (pli + 2) % 3;
          }
                
          /* E. Assign NQRS[qti ][pli ] the value NQRS[qtj ][plj ]. */
          NQRS[qti][pli] = NQRS[qtj][plj];
                
          /* F. Assign QRSIZES[qti ][pli ] the values in QRSIZES[qtj ][plj ]. */
          QRSIZES[qti][pli] = QRSIZES[qtj][plj];
                
          /* G. Assign QRBMIS[qti ][pli ] the values in QRBMIS[qtj ][plj ]. */
          QRBMIS[qti][pli] = QRBMIS[qtj][plj];
                
        } else {
          /* Else, NEWQR is one, which indicates that we are defining a new
             set of quant ranges. In that case: */ 
                
          /*A. Assign qri the value zero. */
          qri = 0;

          /*B. Assign qi the value zero. */
          qi = 0;
                
          /* C. Read an ilog(NBMS - 1)-bit unsigned integer as
                QRBMIS[qti ][pli ][qri ]. If this is greater than or equal to
                NBMS, stop. The stream is undecodable. */
          QRBMIS[qti][pli][qri] = opb.readB(ilog(NBMS - 1));
          if(QRBMIS[qti][pli][qri] >= NBMS) {
            System.out.println("bad header (1)");
            return Result.BADHEADER;
          }
                
          do {
            /* D. Read an ilog(62 - qi )-bit unsigned integer. Assign
                  QRSIZES[qti ][pli ][qri ] the value read, plus one. */
            QRSIZES[qti][pli][qri] = opb.readB(ilog(62 - qi)) + 1;
                
            /* E. Assign qi the value qi + QRSIZES[qti ][pli ][qri ]. */
            qi = qi + QRSIZES[qti][pli][qri];
                
            /* F. Assign qri the value qri + 1. */
            qri = qri + 1;
                
            /* G. Read an ilog(NBMS - 1)-bit unsigned integer as
                  QRBMIS[qti ][pli ][qri ]. */
            QRBMIS[qti][pli][qri] = opb.readB(ilog(NBMS - 1));
                
            /* H. If qi is less than 63, go back to step 7(a)ivD. */
          } while(qi < 63);
                
            /* I. If qi is greater than 63, stop. The stream is undecodable. */
            if(qi > 63) {
              System.out.println("bad header (2): " + qi);
              return Result.BADHEADER;
            }
                
            /* J. Assign NQRS[qti ][pli ] the value qri . */
            NQRS[qti][pli] = qri;
        }
      }
    }
    
    /* Compute all 384 matrices */
    for(int coding = 0; coding < 2; ++coding) {
      for(int plane = 0; plane < 3; ++plane) {
        for(int quality = 0; quality < 64; ++quality) {
          short[] scaledmat = compQuantMatrix(ci.AcScaleFactorTable, ci.DcScaleFactorTable, ci.qmats, NQRS, QRSIZES, QRBMIS, coding, plane, quality);
          for(int coeff = 0; coeff < 64; ++coeff) {
            int j = Constants.dequant_index[coeff];
            ci.dequant_tables[coding][plane][quality][coeff] = scaledmat[j];
          }
        }
      }
    }
  
    return 0;
  }
  
  static short[] compQuantMatrix(int[] ACSCALE, short[] DCSCALE, short[] BMS, int[][] NQRS, 
          int[][][] QRSIZES, int[][][] QRBMIS, int qti, int pli, int qi) {

    /* Variable names according to Theora spec where it makes sense.
     * I *know* this may violate Java coding style rules, but I consider
     * readability against the Theora spec to be more important */
     
    short[] QMAT = new short[64];
    int qri, qrj;
    /* 1. Assign qri the index of a quant range such that
     \qi \ge \sum_{\qrj=0}^{\qri-1} QRSIZES[\qti][\pli][\qrj],
     and
     \qi \le \sum_{\qrj=0}^{\qri} QRSIZES[\qti][\pli][\qrj],
     */
    for(qri = 0; qri < 63; ++qri) {
      int sum1 = 0;
      for(qrj = 0; qrj < qri; ++qrj) {
        sum1 += QRSIZES[qti][pli][qrj];
      }
          
      int sum2 = 0;
      for(qrj = 0; qrj <= qri; ++qrj) {
        sum2 += QRSIZES[qti][pli][qrj];
      }
          
      if(qi >= sum1 && qi <= sum2)
        break;
    }
      
    /* 2. Assign QISTART the value
     \sum_{\qrj=0}^{\qri-1} QRSIZES[\qti][\pli][\qrj].
    */
    int QISTART = 0;
    for(qrj = 0; qrj < qri; ++qrj) {
      QISTART += QRSIZES[qti][pli][qrj];
    }
      
    /* 3. Assign QIEND the value
     \sum_{\qrj=0}^{\qri} QRSIZES[\qti][\pli][\qrj].
    */ 
    int QIEND = 0;
    for(qrj = 0; qrj <= qri; ++qrj) {
      QIEND += QRSIZES[qti][pli][qrj];
    }
      
    /* 4. Assign bmi the value QRBMIS[qti ][pli ][qri ]. */
    int bmi = QRBMIS[qti][pli][qri];
      
    /* 5. Assign bmj the value QRBMIS[qti ][pli ][qri + 1]. */
    int bmj = QRBMIS[qti][pli][qri + 1];
      
    int[] BM = new int[64];
    int QMIN;
    /* 6. For each consecutive value of ci from 0 to 63, inclusive: */
    for(int ci = 0; ci < 64; ++ci) {
      /* (a) Assign BM[ci ] the value
              (2 ∗ (QIEND − qi ) ∗ BMS[bmi ][ci ]
               + 2 ∗ (qi − QISTART) ∗ BMS[bmj ][ci ]
               + QRSIZES[qti ][pli ][qri ])//(2 ∗ QRSIZES[qti ][pli ][qri ]) */
      BM[ci] = (2 * (QIEND - qi) * BMS[(bmi<<6) + ci]
              + 2 * (qi - QISTART) * BMS[(bmj<<6) + ci]
              + QRSIZES[qti][pli][qri]) / (2 * QRSIZES[qti][pli][qri]);
          
      /* (b) Assign QMIN the value given by Table 6.18 according to qti and ci . */
      if(ci == 0 && qti == 0)
        QMIN = 16;
      else if(ci > 0 && qti == 0)
        QMIN = 8;
      else if(ci == 0 && qti == 1)
        QMIN = 32;
      else 
        QMIN = 16;
          
      int QSCALE;
      if(ci == 0) {
        /* (c) If ci equals zero, assign QSCALE the value DCSCALE[qi ]. */
        QSCALE = DCSCALE[qi];
      } else {
        /* (d) Else, assign QSCALE the value ACSCALE[qi ]. */
        QSCALE = ACSCALE[qi];
      }
          
      /*(e) Assign QMAT[ci ] the value
        max(QMIN, min((QSCALE ∗ BM[ci ]//100) ∗ 4, 4096)). */
          
      QMAT[ci] = (short)Math.max(QMIN, Math.min((QSCALE * BM[ci]/100) * 4,4096));
    }
      
    return QMAT;
  }

}