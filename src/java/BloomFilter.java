/**
 *
 * Copyright (c) 2005, European Commission project OneLab under contract 034819 (http://www.one-lab.org)
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or 
 * without modification, are permitted provided that the following 
 * conditions are met:
 *  - Redistributions of source code must retain the above copyright 
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright 
 *    notice, this list of conditions and the following disclaimer in 
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of the University Catholique de Louvain - UCL
 *    nor the names of its contributors may be used to endorse or 
 *    promote products derived from this software without specific prior 
 *    written permission.
 *    
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT 
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS 
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE 
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, 
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, 
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER 
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT 
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN 
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 */
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;


/**
 * Implements a <i>Bloom filter</i>, as defined by Bloom in 1970.
 * <p>
 * The Bloom filter is a data structure that was introduced in 1970 and that has been adopted by 
 * the networking research community in the past decade thanks to the bandwidth efficiencies that it
 * offers for the transmission of set membership information between networked hosts.  A sender encodes 
 * the information into a bit vector, the Bloom filter, that is more compact than a conventional 
 * representation. Computation and space costs for construction are linear in the number of elements.  
 * The receiver uses the filter to test whether various elements are members of the set. Though the 
 * filter will occasionally return a false positive, it will never return a false negative. When creating 
 * the filter, the sender can choose its desired point in a trade-off between the false positive rate and the size. 
 * 
 * contract <a href="http://www.one-lab.org">European Commission One-Lab Project 034819</a>.
 *
 * @version 1.0 - 2 Feb. 07
 * 
 * @see org.onelab.filter.Filter The general behavior of a filter
 * 
 * @see <a href="http://portal.acm.org/citation.cfm?id=362692&dl=ACM&coll=portal">Space/Time Trade-Offs in Hash Coding with Allowable Errors</a>
 */
public class BloomFilter extends Filter {
  
  public static double falsePositiveRate(int numHashes, int vectorSize, long numElements) {
    int k = numHashes;
    int m = vectorSize;
    long n = numElements;
    
    return Math.pow(1.0 - Math.exp(-k*1.0*n/m),k);
  }
  
  /** The bit vector. */
  RapBitSet bits;
  long numElems = 0;

  /** Default constructor - use with readFields */
  public BloomFilter() {
    super();
  }
  
  /**
   * Constructor
   * @param vectorSize The vector size of <i>this</i> filter.
   * @param nbHash The number of hash function to consider.
   * @param hashType type of the hashing function (see {@link Hash}).
   */
  public BloomFilter(int vectorSize, int nbHash, int hashType){
    super(vectorSize, nbHash, hashType);

    bits = new RapBitSet(this.vectorSize);
  }//end constructor

  @Override
  public void add(Key key) {
    if(key == null) {
      throw new NullPointerException("key cannot be null");
    }

    int[] h = hash.hash(key);
    hash.clear();

    for(int i = 0; i < nbHash; i++) {
      bits.set(h[i]);
    }
    numElems++;
  }


  @Override
  public boolean membershipTest(Key key){
    if(key == null) {
      throw new NullPointerException("key cannot be null");
    }

    int[] h = hash.hash(key);
    hash.clear();
    for(int i = 0; i < nbHash; i++) {
      if(!bits.get(h[i])) {
        return false;
      }
    }
    return true;
  }//end memberhsipTest()

  
  /**
   * @return size of the the bloomfilter
   */
  public int getVectorSize() {
    return this.vectorSize;
  }
  
  public double getFalsePositiveRate() {
    int k = nbHash;
    int m = getVectorSize();
    long n = numElems;
    
    return BloomFilter.falsePositiveRate(k, m, n);
  }
  
  public void acceptAll() {
    bits.fill();
  }

  // Writable

  @Override
  public void write(DataOutput out) throws IOException {
    super.write(out);
    out.writeLong(numElems);
    out.write(bits.getRaw());
  }

  @Override
  public void readFields(DataInput in) throws IOException {
    super.readFields(in);
    numElems = in.readLong();
    byte[] bytes = new byte[getNBytes()];
    in.readFully(bytes);    
    bits = new RapBitSet(this.vectorSize, bytes);
  }
  
  /* @return number of bytes needed to hold bit vector */
  private int getNBytes() {
    return (vectorSize + 7) / 8;
  }
}
