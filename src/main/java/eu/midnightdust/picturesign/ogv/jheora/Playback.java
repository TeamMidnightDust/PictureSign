/* Jheora
 * Copyright (C) 2004 Fluendo S.L.
 *  
 * Written by: 2004 Wim Taymans <wim@fluendo.com>
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

public class Playback 
{
  /* Different key frame types/methods */
  private static final int DCT_KEY_FRAME = 0;

  //oggpack_buffer *opb;
  OggBuffer  opb = new OggBuffer();;
  TheoraInfo     info;
  /* how far do we shift the granulepos to seperate out P frame counts? */
  int             keyframe_granule_shift;


  /***********************************************************************/
  /* Decoder and Frame Type Information */

  int           DecoderErrorCode;
  int           FramesHaveBeenSkipped;

  int           PostProcessEnabled;
  int           PostProcessingLevel;    /* Perform post processing */

  /* Frame Info */
  byte 		FrameType;
  byte 		KeyFrameType;
  int           QualitySetting;
  int           FrameQIndex;            /* Quality specified as a
                                           table index */
  //int           ThisFrameQualityValue;  /* Quality value for this frame  */
  //int           LastFrameQualityValue;  /* Last Frame's Quality */
  int     	CodedBlockIndex;        /* Number of Coded Blocks */
  int           CodedBlocksThisFrame;   /* Index into coded blocks */
  int           FrameSize;              /* The number of bytes in the frame. */
  
  int[]         frameQIS = new int[3];
  int           frameNQIS; /* number of quality indices this frame uses */


  /**********************************************************************/
  /* Frame Size & Index Information */

  int           YPlaneSize;
  int           UVPlaneSize;
  int           YStride;
  int           UVStride;
  int           VFragments;
  int           HFragments;
  int           UnitFragments;
  int           YPlaneFragments;
  int           UVPlaneFragments;
  
  int           UVShiftX;       /* 1 unless Info.pixel_fmt == TH_PF_444 */
  int           UVShiftY;       /* 0 unless Info.pixel_fmt == TH_PF_420 */

  int           ReconYPlaneSize;
  int           ReconUVPlaneSize;

  int           YDataOffset;
  int           UDataOffset;
  int           VDataOffset;
  int           ReconYDataOffset;
  int           ReconUDataOffset;
  int           ReconVDataOffset;
  int           YSuperBlocks;   /* Number of SuperBlocks in a Y frame */
  int           UVSuperBlocks;  /* Number of SuperBlocks in a U or V frame */
  int           SuperBlocks;    /* Total number of SuperBlocks in a
                                   Y,U,V frame */

  int           YSBRows;        /* Number of rows of SuperBlocks in a
                                   Y frame */
  int           YSBCols;        /* Number of cols of SuperBlocks in a
                                   Y frame */
  int           UVSBRows;       /* Number of rows of SuperBlocks in a
                                   U or V frame */
  int           UVSBCols;       /* Number of cols of SuperBlocks in a
                                   U or V frame */

  int           YMacroBlocks;   /* Number of Macro-Blocks in Y component */
  int           UVMacroBlocks;  /* Number of Macro-Blocks in U/V component */
  int           MacroBlocks;    /* Total number of Macro-Blocks */

  /**********************************************************************/
  /* Frames  */
  short[] 	ThisFrameRecon;
  short[] 	GoldenFrame;
  short[] 	LastFrameRecon;
  short[] 	PostProcessBuffer;

  /**********************************************************************/
  /* Fragment Information */
  int[]         pixel_index_table;        /* start address of first
                                              pixel of fragment in
                                              source */
  int[]		recon_pixel_index_table;  /* start address of first
                                              pixel in recon buffer */

  byte[] 	display_fragments;        /* Fragment update map */
  int[]  	CodedBlockList;           /* A list of fragment indices for
                                              coded blocks. */
  MotionVector[] FragMVect;                /* fragment motion vectors */

  int[]         FragTokenCounts;          /* Number of tokens per fragment */
  int[]		FragQIndex;               /* Fragment Quality used in
                                              PostProcess */

  byte[] 	FragCoefEOB;               /* Position of last non 0 coef
                                                within QFragData */
  short[][] 	QFragData;            /* Fragment Coefficients
                                               Array Pointers */
  byte[]        FragQs;                 /* per block quantizers */
  CodingMode[] 	FragCodingMethod;          /* coding method for the
                                               fragment */

  /***********************************************************************/
  /* Macro Block and SuperBlock Information */
  BlockMapping  BlockMap;          /* super block + sub macro
                                                   block + sub frag ->
                                                   FragIndex */

  /* Coded flag arrays and counters for them */
  byte[] 	SBCodedFlags;
  byte[] 	SBFullyFlags;
  byte[] 	MBCodedFlags;
  byte[] 	MBFullyFlags;

  /**********************************************************************/

  Coordinate[]  FragCoordinates;
  FrArray 	frArray = new FrArray();
  Filter 	filter = new Filter();

  
  /* quality index for each block */
  byte[]        blockQ;

  /* Dequantiser and rounding tables */
  int[]   	quant_index = new int[64];

  HuffEntry[]   HuffRoot_VP3x = new HuffEntry[Huffman.NUM_HUFF_TABLES];
  int[][] 	HuffCodeArray_VP3x;
  byte[][] 	HuffCodeLengthArray_VP3x;
  byte[]   	ExtraBitLengths_VP3x;
 

  public void clear()
  {
    if (opb != null) {
      opb = null;
    }
  }

  private static int ilog (long v)
  {
    int ret=0;

    while (v != 0) {
      ret++;
      v>>=1;
    }
    return ret;
  }

  public Playback (TheoraInfo ci)
  {
    info = ci;

    DecoderErrorCode = 0;
    KeyFrameType = DCT_KEY_FRAME;
    FramesHaveBeenSkipped = 0;

    FrInit.InitFrameDetails(this);

    keyframe_granule_shift = ilog(ci.keyframe_frequency_force-1);
    //LastFrameQualityValue = 0;

    /* Initialise version specific quantiser and in-loop filter values */
    filter.copyFilterTables(ci);

    /* Huffman setup */
    initHuffmanTrees(ci);
  }

  public int getFrameType() {
    return FrameType;
  }

  void setFrameType(byte FrType ){
    /* Set the appropriate frame type according to the request. */
    switch ( FrType ){
      case Constants.BASE_FRAME:
        FrameType = FrType;
	break;
      default:
        FrameType = FrType;
        break;
    }
  }

  public void clearHuffmanSet()
  {
    Huffman.clearHuffmanTrees(HuffRoot_VP3x);

    HuffCodeArray_VP3x = null;
    HuffCodeLengthArray_VP3x = null;
  }

  public void initHuffmanSet()
  {
    clearHuffmanSet();

    ExtraBitLengths_VP3x = HuffTables.ExtraBitLengths_VP31;

    HuffCodeArray_VP3x = new int[Huffman.NUM_HUFF_TABLES][Huffman.MAX_ENTROPY_TOKENS];
    HuffCodeLengthArray_VP3x = new byte[Huffman.NUM_HUFF_TABLES][Huffman.MAX_ENTROPY_TOKENS];

    for (int i = 0; i < Huffman.NUM_HUFF_TABLES; i++ ){
      Huffman.buildHuffmanTree(HuffRoot_VP3x,
                       HuffCodeArray_VP3x[i],
                       HuffCodeLengthArray_VP3x[i],
                       i, HuffTables.FrequencyCounts_VP3[i]);
    }
  }

  public int readHuffmanTrees(TheoraInfo ci, OggBuffer opb) {
    int i;
    for (i=0; i<Huffman.NUM_HUFF_TABLES; i++) {
       int ret;
       ci.HuffRoot[i] = new HuffEntry();
       ret = ci.HuffRoot[i].read(0, opb);
       if (ret != 0) 
         return ret;
    }
    return 0;
  }

  public void initHuffmanTrees(TheoraInfo ci) 
  {
    int i;
    ExtraBitLengths_VP3x = HuffTables.ExtraBitLengths_VP31;
    for(i=0; i<Huffman.NUM_HUFF_TABLES; i++){
      HuffRoot_VP3x[i] = ci.HuffRoot[i].copy();
    }
  }
}
