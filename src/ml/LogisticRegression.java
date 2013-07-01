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

package ml;

import java.io.IOException;
import java.util.Vector;

import ml.LBFGS.ExceptionWithIflag;

import base.Example;
import base.WeightParameter;


public class LogisticRegression {
	
	public static double optimize( Vector<Example> data , WeightParameter param, double lambda , double eps , int maxnfn ) throws IOException {
		int m = 5;
		double f = 0, xtol = 1e-30;
		boolean diagco = false;
		int[] iprint = new int[2];
		int[] iflag = new int[1];
		
		int n = param.weightvector.length;
		for( Example E : data )
			n = Math.max( n , E.fsize() + 1 );
		
		double[] x  = new double[n];
		double[] g = new double[n]; 
		double[] diag = new double[n];
		
		iprint[0] = 1;
		iprint[1] = 0;
		iflag[0] = 0;
		
		for( int i = 0; i < param.weightvector.length; ++i )
			x[i] = param.weightvector[i];
		
		LBFGS opt = new LBFGS();
		while( maxnfn > 0 ) {
			f = FunctionGradient( x , g , data , param , lambda );
			try {
				opt.lbfgs(n, m, x, f, g, diagco, diag , iprint, eps, xtol, iflag);
			} catch (ExceptionWithIflag e1) {
				e1.printStackTrace();
				System.out.print(" [ GC : LBFGS dint achieve specified tolerance  ]");
				break;
			}
			if( iflag[0] <= 0 ) break;
			maxnfn--;
		}
		
    	// Store x into current.
		param.weightvector = new float[x.length];
		double nn = 0;
		for( int i = 0; i < x.length; ++i ) {
			param.weightvector[i] = (float) x[i];
			nn += x[i]*x[i];
		}
		if ( Double.isNaN(nn) || Double.isInfinite(nn) ) {
			System.out.println(" The norm of the vector is Nan. Something is wrong");
			System.err.println(" The norm of the vector is Nan. Something is wrong");
			throw new IOException();
		}
		return f;
	}

	private static double FunctionGradient( double[] x, double[] G , Vector<Example> data , WeightParameter param , double lambda ){
		double f = 0;
		
		G[0] = 0;
		for( int i = 1; i < x.length; ++i ) {
			G[i] = lambda * x[i];
			f += lambda/2*x[i]*x[i];
		}

		for( Example e : data ) {
			int y = -1;
			for ( int l : e.labels ) if ( l == param.node ) y = 1;

			double wx = x[0];  
			for ( int i = 0; i < e.fids.length; ++i )
				wx += x[ e.fids[i] ] * e.fvals[i];		
			double pr = 1.0 / ( 1.0 + Math.exp(y*wx) );

			// Gradient through data
			for ( int i = 0; i < e.fids.length; ++i )
				G[ e.fids[i] ] += -y * pr * e.fvals[i];
			G[0] += -y*pr;
			
			double es = Math.exp(-y*wx), add = 0;
			if ( Double.isInfinite(es) )
				add = Double.MAX_VALUE/1e50;
			else
				add = Math.log( 1 + es );

			if ( Double.isNaN(add) || Double.isInfinite(add) ) {
				System.out.println(" [Nan or Inf Problem] pr = " + pr + " i = " + e.docid + " y = " + y + " add = " + add );
				System.exit(0);
			}
			f += add;
		}
		return f;
	}
}
