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

public class TheoraInfo {
  public int width;
  public int height;
  public int frame_width;
  public int frame_height;
  public int offset_x;
  public int offset_y;
  public int fps_numerator;
  public int fps_denominator;
  public int aspect_numerator;
  public int aspect_denominator;
  public Colorspace colorspace;
  public PixelFormat pixel_fmt;
  public int  target_bitrate;
  public int  quality;
  public int  quick_p;  /* quick encode/decode */

  /* decode only */
  public byte version_major;
  public byte version_minor;
  public byte version_subminor;

  public int   keyframe_granule_shift;
  public long  keyframe_frequency_force;

  /* codec_setup_info */
  short[][][][] dequant_tables = new short[2][3][64][64];       
  int[] AcScaleFactorTable = new int[Constants.Q_TABLE_SIZE];
  short[] DcScaleFactorTable = new short[Constants.Q_TABLE_SIZE];
  int MaxQMatrixIndex;
  short[] qmats;
  
  HuffEntry[] HuffRoot = new HuffEntry[Huffman.NUM_HUFF_TABLES];
  byte[] LoopFilterLimitValues = new byte[Constants.Q_TABLE_SIZE];

  private static void _tp_readbuffer(OggBuffer opb, byte[] buf, int len)
  {
    for (int i=0; i<len; i++) {
      buf[i] = (byte)opb.readB(8);
    }
  }

  private static int _tp_readlsbint(OggBuffer opb)
  {
    int value;

    value  = opb.readB(8);
    value |= opb.readB(8) << 8;
    value |= opb.readB(8) << 16;
    value |= opb.readB(8) << 24;

    return value;
  }

  private int unpackInfo(OggBuffer opb){
    version_major = (byte)opb.readB(8);
    version_minor = (byte)opb.readB(8);
    version_subminor = (byte)opb.readB(8);

    if (version_major != Version.VERSION_MAJOR) 
      return Result.VERSION;
    if (version_minor > Version.VERSION_MINOR) 
      return Result.VERSION;

    width  = (int)(opb.readB(16)<<4);
    height = (int)(opb.readB(16)<<4);
    frame_width = (int)opb.readB(24);
    frame_height = (int)opb.readB(24);
    offset_x = (int)opb.readB(8);
    offset_y = (int)opb.readB(8);

    /* Invert the offset so that it is from the top down */
    offset_y = height-frame_height-offset_y;

    fps_numerator = opb.readB(32);
    fps_denominator = opb.readB(32);
    aspect_numerator = opb.readB(24);
    aspect_denominator = opb.readB(24);

    colorspace = Colorspace.spaces[opb.readB(8)];
    target_bitrate = opb.readB(24);
    quality = opb.readB(6);

    keyframe_granule_shift = opb.readB(5);
    keyframe_frequency_force = 1<<keyframe_granule_shift;
    
    pixel_fmt = PixelFormat.formats[opb.readB(2)];
    if (pixel_fmt==PixelFormat.TH_PF_RSVD)
      return (Result.BADHEADER);

    /* spare configuration bits */
    if (opb.readB(3) == -1)
      return (Result.BADHEADER);

    return(0);
  }

  static int unpackComment (TheoraComment tc, OggBuffer opb)
  {
    int i;
    int len;
    byte[] tmp;
    int comments;

    len = _tp_readlsbint(opb);
    if(len<0)
      return(Result.BADHEADER);

    tmp=new byte[len];
    _tp_readbuffer(opb, tmp, len);
    tc.vendor=new String(tmp);

    comments = _tp_readlsbint(opb);
    if(comments<0) {
      tc.clear();
      return Result.BADHEADER;
    }
    tc.user_comments=new String[comments];
    for(i=0;i<comments;i++){
      len = _tp_readlsbint(opb);
      if(len<0) {
        tc.clear();
        return Result.BADHEADER;
      }

      tmp=new byte[len];
      _tp_readbuffer(opb,tmp,len);
      tc.user_comments[i]=new String(tmp);
    }
    return 0;
  }

  /* handle the in-loop filter limit value table */
  private int readFilterTables(OggBuffer opb) 
  {
    int bits = opb.readB(3);
    for (int i=0; i<Constants.Q_TABLE_SIZE; i++) {
      int value = opb.readB(bits);

      LoopFilterLimitValues[i] = (byte) value;
    }
    if (bits<0) 
      return Result.BADHEADER;

    return 0;
  }


  private int unpackTables (OggBuffer opb)
  {
    int ret;

    ret = readFilterTables(opb);
    if (ret != 0)
      return ret;
    ret = Quant.readQTables(this, opb);
    if (ret != 0)
      return ret;
    ret = Huffman.readHuffmanTrees(HuffRoot, opb);
    if (ret != 0)
      return ret;

    return ret;
  }

  public void clear() {
    qmats = null;
    
    Huffman.clearHuffmanTrees(HuffRoot);
  }

  public int decodeHeader (TheoraComment cc, OggPacket op)
  {
    long ret;
    OggBuffer opb = new OggBuffer();

    opb.readinit (op.packet_base, op.packet, op.bytes);
  
    {
      byte[] id = new byte[6];
      int typeflag;
    
      typeflag = opb.readB(8);
      if((typeflag & 0x80) == 0) {
        return Result.NOTFORMAT;
      }

      _tp_readbuffer(opb,id,6);
      if (!"theora".equals(new String(id))) {
        return Result.NOTFORMAT;
      }

      switch(typeflag){
      case 0x80:
        if(op.b_o_s == 0){
          /* Not the initial packet */
          return Result.BADHEADER;
        }
        if(version_major!=0){
          /* previously initialized info header */
          return Result.BADHEADER;
        }

        ret = unpackInfo(opb);
        return (int)ret;

      case 0x81:
        if(version_major==0){
          /* um... we didn't get the initial header */
          return Result.BADHEADER;
        }

        ret = unpackComment(cc,opb);
        return (int)ret;

      case 0x82:
        if(version_major==0 || cc.vendor==null){
          /* um... we didn't get the initial header or comments yet */
          return Result.BADHEADER;
        }

        ret = unpackTables(opb);
        return (int)ret;
    
      default:
        if(version_major==0 || cc.vendor==null || 
           HuffRoot[0]==null)
	{
          /* we haven't gotten the three required headers */
          return Result.BADHEADER;
        }
        /* ignore any trailing header packets for forward compatibility */
        return Result.NEWPACKET;
      }
    }
  }
}
