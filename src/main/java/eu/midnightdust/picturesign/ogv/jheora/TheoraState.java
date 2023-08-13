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

public class TheoraState 
{
  long granulepos;

  private Playback pbi;
  private Decode dec;

  public void clear()
  {
    if(pbi != null){
      pbi.info.clear();
      pbi.clearHuffmanSet();
      FrInit.ClearFragmentInfo(pbi);
      FrInit.ClearFrameInfo(pbi);
      pbi.clear();
    }
    pbi = null;
  }

  public int decodeInit(TheoraInfo ci)
  {
    pbi = new Playback(ci);
    dec = new Decode(pbi);
    granulepos=-1;

    return(0);
  }

  public boolean isKeyframe (OggPacket op)
  {
    return (op.packet_base[op.packet] & 0x40) == 0;
  }

  public int decodePacketin (OggPacket op)
  {
    long ret;

    pbi.DecoderErrorCode = 0;

    if (op.bytes>0) {
      pbi.opb.readinit(op.packet_base, op.packet, op.bytes);

      /* verify that this is a video frame */
      ret = pbi.opb.readB(1);

      if (ret==0) {
        try {
          ret=dec.loadAndDecode();
        } catch(Exception e) {
          /* If lock onto the bitstream is lost all sort of Exceptions can occur.
           * The bitstream damage may be local, so the next packet may be okay. */
          e.printStackTrace();
          return Result.BADPACKET;
        }

        if(ret != 0)
          return (int) ret;
 
      } else {
        return Result.BADPACKET;
      }
   }
   if(op.granulepos>-1)
      granulepos=op.granulepos;
   else{
      if(granulepos==-1){
        granulepos=0;
      }
      else {
        if ((op.bytes>0) && (pbi.FrameType == Constants.BASE_FRAME)){
          long frames= granulepos & ((1<<pbi.keyframe_granule_shift)-1);
          granulepos>>=pbi.keyframe_granule_shift;
          granulepos+=frames+1;
          granulepos<<=pbi.keyframe_granule_shift;
        }else
          granulepos++;
      }
   }

        return(0);
  }

  public int decodeYUVout (YUVBuffer yuv)
  {
    yuv.y_width = pbi.info.width;
    yuv.y_height = pbi.info.height;
    yuv.y_stride = pbi.YStride;

    yuv.uv_width = pbi.info.width >> pbi.UVShiftX;
    yuv.uv_height = pbi.info.height >> pbi.UVShiftY;
    yuv.uv_stride = pbi.UVStride;

    if(pbi.PostProcessingLevel != 0){
      yuv.data = pbi.PostProcessBuffer;
    }else{
      yuv.data = pbi.LastFrameRecon;
    }
    yuv.y_offset = pbi.ReconYDataOffset;
    yuv.u_offset = pbi.ReconUDataOffset;
    yuv.v_offset = pbi.ReconVDataOffset;
  
    /* we must flip the internal representation,
       so make the stride negative and start at the end */
    yuv.y_offset += yuv.y_stride * (yuv.y_height - 1);
    yuv.u_offset += yuv.uv_stride * (yuv.uv_height - 1);
    yuv.v_offset += yuv.uv_stride * (yuv.uv_height - 1);
    yuv.y_stride = - yuv.y_stride;
    yuv.uv_stride = - yuv.uv_stride;

    yuv.newPixels();
  
    return 0;
  }

  /* returns, in seconds, absolute time of current packet in given
     logical stream */
  public double granuleTime(long granulepos)
  {
    if(granulepos>=0){
      long iframe=granulepos>>pbi.keyframe_granule_shift;
      long pframe=granulepos-(iframe<<pbi.keyframe_granule_shift);

      return (iframe+pframe)*
        ((double)pbi.info.fps_denominator/pbi.info.fps_numerator);
    }
    return(-1);
  }
}
