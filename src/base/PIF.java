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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.WritableComparable;

public class PIF implements WritableComparable<PIF> {
	public int index = 0;
	public float value = 0;
	
	public PIF ( ){		
	}
	
	public PIF ( PIF a ) {
		index = a.index;
		value = a.value;
	}
	
	public PIF ( int _index , float _value ) {
		index = _index;
		value = _value;
	}

	public void readFields(DataInput in) throws IOException {
		index = in.readInt();
		value = in.readFloat();
	}

	public void write(DataOutput out) throws IOException {
		out.writeInt( index );
		out.writeFloat( value );		
	}
	
	public String toString(){
		return index+":"+value+" ";
	}

	public int compareTo(PIF other) {
		if ( value < other.value ) return -1;
		if ( value == other.value ) return 0;
		else return +1;
	}
	
}