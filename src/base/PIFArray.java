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
import java.util.Vector;

import org.apache.hadoop.io.Writable;

public class PIFArray implements Writable
{
	public Vector<PIF> pifs;
    public PIFArray(){
    	pifs = new Vector<PIF>();
    }
    
	public void readFields(DataInput in) throws IOException {
		int n = in.readInt();
		pifs.clear();

		PIF a = new PIF();
		for ( int i = 0;i < n; ++i ){
			a.readFields(in);
			pifs.add( new PIF(a.index,a.value) );
		}
	}

	public void write(DataOutput out) throws IOException {
		out.writeInt(pifs.size());
		for ( PIF a : pifs ) {
			a.write(out);
		}
	}
	
	public String toString() {
		String ret = "";
		for ( PIF a : pifs ) {
			ret += a.toString(); 
		}
		return ret;
	}
}