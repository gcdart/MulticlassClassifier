/* Copyright (C) 2013, Siddharth Gopal (gcdart AT gmail)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of version 2.1 of the GNU Lesser General Public License
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA, 02111-1307, USA,
 * or visit the GNU web site, www.gnu.org.
 */

package base;

import java.io.*;
import org.apache.hadoop.io.Writable;


public class  WeightParameter implements Writable {
	public int node;
	public float[] weightvector;	
	
	public WeightParameter(){
		clear();
	}


	public WeightParameter( final WeightParameter x ){
		node = x.node;
		weightvector = x.weightvector.clone();		
	}
	
	public void clear(){
		node = -1;
		weightvector = new float[1];
		weightvector[0] = 0;
	}
	
	public void readFields(DataInput in) throws IOException {
		node = in.readInt();
		int n = in.readInt();
		
		weightvector = new float[n];
		for( int i = 0; i < n; ++i )
			weightvector[i] = in.readFloat();
	}
	
	public void write(DataOutput out) throws IOException {
		out.writeInt(node);
		
		out.writeInt( weightvector.length );
		for( int i = 0; i < weightvector.length; ++i )
			out.writeFloat(weightvector[i]);		
	}
	
	public double getScore( Example E ) {
		double score = weightvector[0];  
		for ( int i = 0; i < E.fids.length; ++i )
			if ( E.fids[i] < weightvector.length )
			score += weightvector[ E.fids[i] ] * E.fvals[i];			
		return score;
	}

	
	public String toString( int x){
		String ret = " Node = " + node  + "\n wv = " + weightvector.toString() + "\n";
		return ret;
	}
	
	public String toString( ){
		String ret = " Node = " + node  + "\n wv = ";
		for ( int i = 0; i < weightvector.length; ++i ) 
			ret += i+":" + weightvector[i]+" ";
		return ret + "\n";
	}

	public double norm() {
		double ret = 0;
		for ( int i = 0; i < weightvector.length; ++i ) 
			ret += weightvector[i] * weightvector[i]; 
		return ret;
	}
}