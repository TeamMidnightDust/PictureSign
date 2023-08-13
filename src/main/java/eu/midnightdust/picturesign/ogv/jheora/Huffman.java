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

public class Huffman
{
  public static final int NUM_HUFF_TABLES        = 80;
  public static final int DC_HUFF_OFFSET         = 0;
  public static final int AC_HUFF_OFFSET         = 16;
  public static final int AC_TABLE_2_THRESH      = 5;
  public static final int AC_TABLE_3_THRESH      = 14;
  public static final int AC_TABLE_4_THRESH      = 27;

  public static final int DC_HUFF_CHOICES        = 16;
  public static final int DC_HUFF_CHOICE_BITS    = 4;

  public static final int AC_HUFF_CHOICES        = 16;
  public static final int AC_HUFF_CHOICE_BITS    = 4;

/* Constants assosciated with entropy tokenisation. */
  public static final int MAX_SINGLE_TOKEN_VALUE = 6;
  public static final int DCT_VAL_CAT2_MIN       = 3;
  public static final int DCT_VAL_CAT3_MIN       = 7;
  public static final int DCT_VAL_CAT4_MIN       = 9;
  public static final int DCT_VAL_CAT5_MIN       = 13;
  public static final int DCT_VAL_CAT6_MIN       = 21;
  public static final int DCT_VAL_CAT7_MIN       = 37;
  public static final int DCT_VAL_CAT8_MIN       = 69;

  public static final int DCT_EOB_TOKEN          = 0;
  public static final int DCT_EOB_PAIR_TOKEN     = 1;
  public static final int DCT_EOB_TRIPLE_TOKEN   = 2;
  public static final int DCT_REPEAT_RUN_TOKEN   = 3;
  public static final int DCT_REPEAT_RUN2_TOKEN  = 4;
  public static final int DCT_REPEAT_RUN3_TOKEN  = 5;
  public static final int DCT_REPEAT_RUN4_TOKEN  = 6;

  public static final int DCT_SHORT_ZRL_TOKEN    = 7;
  public static final int DCT_ZRL_TOKEN          = 8;

  public static final int ONE_TOKEN              = 9;       /* Special tokens for -1,1,-2,2 */
  public static final int MINUS_ONE_TOKEN        = 10;
  public static final int TWO_TOKEN              = 11;
  public static final int MINUS_TWO_TOKEN        = 12;

  public static final int LOW_VAL_TOKENS         = (MINUS_TWO_TOKEN + 1);
  public static final int DCT_VAL_CATEGORY3      = (LOW_VAL_TOKENS + 4);
  public static final int DCT_VAL_CATEGORY4      = (DCT_VAL_CATEGORY3 + 1);
  public static final int DCT_VAL_CATEGORY5      = (DCT_VAL_CATEGORY4 + 1);
  public static final int DCT_VAL_CATEGORY6      = (DCT_VAL_CATEGORY5 + 1);
  public static final int DCT_VAL_CATEGORY7      = (DCT_VAL_CATEGORY6 + 1);
  public static final int DCT_VAL_CATEGORY8      = (DCT_VAL_CATEGORY7 + 1);

  public static final int DCT_RUN_CATEGORY1      = (DCT_VAL_CATEGORY8 + 1);
  public static final int DCT_RUN_CATEGORY1B     = (DCT_RUN_CATEGORY1 + 5);
  public static final int DCT_RUN_CATEGORY1C     = (DCT_RUN_CATEGORY1B + 1);
  public static final int DCT_RUN_CATEGORY2      = (DCT_RUN_CATEGORY1C + 1);

/* 32 */
  public static final int MAX_ENTROPY_TOKENS     = (DCT_RUN_CATEGORY2 + 2);

  private static void createHuffmanList (HuffEntry[] huffRoot,
                              int hIndex, short[] freqList) {
    HuffEntry entry_ptr;
    HuffEntry search_ptr;

    /* Create a HUFF entry for token zero. */
    huffRoot[hIndex] = new HuffEntry();
    huffRoot[hIndex].previous = null;
    huffRoot[hIndex].next = null;
    huffRoot[hIndex].Child[0] = null;
    huffRoot[hIndex].Child[1] = null;
    huffRoot[hIndex].value = 0;
    huffRoot[hIndex].frequency = freqList[0];

    if ( huffRoot[hIndex].frequency == 0 )
      huffRoot[hIndex].frequency = 1;

    /* Now add entries for all the other possible tokens. */
    for (int i = 1; i < Huffman.MAX_ENTROPY_TOKENS; i++) {
      entry_ptr = new HuffEntry();
      entry_ptr.value = i;
      entry_ptr.frequency = freqList[i];
      entry_ptr.Child[0] = null;
      entry_ptr.Child[1] = null;
  
      /* Force min value of 1. This prevents the tree getting too deep. */
      if ( entry_ptr.frequency == 0 )
        entry_ptr.frequency = 1;
  
      if ( entry_ptr.frequency <= huffRoot[hIndex].frequency ){
        entry_ptr.next = huffRoot[hIndex];
        huffRoot[hIndex].previous = entry_ptr;
        entry_ptr.previous = null;
        huffRoot[hIndex] = entry_ptr;
      }
      else
      {
        search_ptr = huffRoot[hIndex];
        while ( (search_ptr.next != null) &&
                (search_ptr.frequency < entry_ptr.frequency) ){
          search_ptr = search_ptr.next;
        }
  
        if ( search_ptr.frequency < entry_ptr.frequency ){
          entry_ptr.next = null;
          entry_ptr.previous = search_ptr;
          search_ptr.next = entry_ptr;
        } 
	else
	{
          entry_ptr.next = search_ptr;
          entry_ptr.previous = search_ptr.previous;
          search_ptr.previous.next = entry_ptr;
          search_ptr.previous = entry_ptr;
        }
      }
    }
  }

  private static void createCodeArray (HuffEntry huffRoot,
                      int[] huffCodeArray,
                      byte[] huffCodeLengthArray,
                      int codeValue,
                      byte codeLength) 
  {
    /* If we are at a leaf then fill in a code array entry. */
    if ((huffRoot.Child[0] == null) && (huffRoot.Child[1] == null)) {
      huffCodeArray[huffRoot.value] = codeValue;
      huffCodeLengthArray[huffRoot.value] = codeLength;
    } else {
      /* Recursive calls to scan down the tree. */
      codeLength++;
      createCodeArray(huffRoot.Child[0], huffCodeArray, huffCodeLengthArray,
                      ((codeValue << 1) + 0), codeLength);
      createCodeArray(huffRoot.Child[1], huffCodeArray, huffCodeLengthArray,
                      ((codeValue << 1) + 1), codeLength);
    }
  }

  public static void buildHuffmanTree (HuffEntry[] huffRoot,
                        int[] huffCodeArray,
                        byte[] huffCodeLengthArray,
                        int hIndex,
                        short[] freqList )
  {
    HuffEntry entry_ptr;
    HuffEntry search_ptr;

    /* First create a sorted linked list representing the frequencies of
       each token. */
    createHuffmanList( huffRoot, hIndex, freqList );

    /* Now build the tree from the list. */

    /* While there are at least two items left in the list. */
    while ( huffRoot[hIndex].next != null ){
      /* Create the new node as the parent of the first two in the list. */
      entry_ptr = new HuffEntry();
      entry_ptr.value = -1;
      entry_ptr.frequency = huffRoot[hIndex].frequency +
        huffRoot[hIndex].next.frequency ;
      entry_ptr.Child[0] = huffRoot[hIndex];
      entry_ptr.Child[1] = huffRoot[hIndex].next;

      /* If there are still more items in the list then insert the new
         node into the list. */
      if (entry_ptr.Child[1].next != null ){
        /* Set up the provisional 'new root' */
        huffRoot[hIndex] = entry_ptr.Child[1].next;
        huffRoot[hIndex].previous = null;

        /* Now scan through the remaining list to insert the new entry
           at the appropriate point. */
        if ( entry_ptr.frequency <= huffRoot[hIndex].frequency ){
          entry_ptr.next = huffRoot[hIndex];
          huffRoot[hIndex].previous = entry_ptr;
          entry_ptr.previous = null;
          huffRoot[hIndex] = entry_ptr;
        }else{
          search_ptr = huffRoot[hIndex];
          while ( (search_ptr.next != null) &&
                (search_ptr.frequency < entry_ptr.frequency) ){
            search_ptr = search_ptr.next;
          }

          if ( search_ptr.frequency < entry_ptr.frequency ){
            entry_ptr.next = null;
            entry_ptr.previous = search_ptr;
            search_ptr.next = entry_ptr;
          }else{
            entry_ptr.next = search_ptr;
            entry_ptr.previous = search_ptr.previous;
            search_ptr.previous.next = entry_ptr;
            search_ptr.previous = entry_ptr;
          }
        }
      }else{
        /* Build has finished. */
        entry_ptr.next = null;
        entry_ptr.previous = null;
        huffRoot[hIndex] = entry_ptr;
      }

      /* Delete the next/previous properties of the children (PROB NOT NEC). */
      entry_ptr.Child[0].next = null;
      entry_ptr.Child[0].previous = null;
      entry_ptr.Child[1].next = null;
      entry_ptr.Child[1].previous = null;

    }

    /* Now build a code array from the tree. */
    createCodeArray( huffRoot[hIndex], huffCodeArray,
                     huffCodeLengthArray, 0, (byte)0);
  }

  public static int readHuffmanTrees(HuffEntry[] huffRoot, OggBuffer opb) {
    int i;
    for (i=0; i<NUM_HUFF_TABLES; i++) {
       int ret;
       huffRoot[i] = new HuffEntry();
       ret = huffRoot[i].read(0, opb);
       if (ret != 0) 
         return ret;
    }
    return 0;
  }

  public static void clearHuffmanTrees(HuffEntry[] huffRoot){
    int i;
    for(i=0; i<Huffman.NUM_HUFF_TABLES; i++) {
      huffRoot[i] = null;
    }
  }
}
