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

/*
 *  Largely re-written from the Dual co-ordinate Descent for L2-regularized L1-SVM 
 *  developed in http://www.csie.ntu.edu.tw/~cjlin/liblinear/
 *   
 *   - Siddharth Gopal gcdart@gmail 
 */

package ml;
import java.util.Random;
import java.util.Vector;

import base.*;

public class BinarySVM {
		final static double EPS = 1e-12;
		
		public static double optimize( Vector<Example> data , WeightParameter param, double C , double eps , int max_iter ) {
			Boolean use_bias = true;
			int n = param.weightvector.length, N = data.size();
			for( Example E : data )
				n = Math.max( n , E.fsize() + 1 );
			param.weightvector = new float[n];

			int[] index = new int[N];
			int[] label = new int[N];
			double[] QD = new double[N];
			double[] alpha = new double[N];
			double PGmax_old = Double.POSITIVE_INFINITY, PGmin_old = Double.NEGATIVE_INFINITY;
			int active_size = N , npos = 0 , nneg = 0;
			Random generator = new Random();

			for( int i = 0; i < N; ++i ) {
				Example E = data.get(i);
				int l = -1;
				for ( int lab : E.labels ) if ( lab == param.node ) l = 1;

				npos += (l > 0 ? 1: 0);
				nneg += (l < 0 ? 1: 0);
				QD[i] = alpha[i] = 0;
				index[i] = i;
				label[i] = l;
				for( int j = 0; j < E.fids.length; ++j ) 
					QD[i] += E.fvals[j] * E.fvals[j];
				if( use_bias ) QD[i] += 1;
			}
			if ( npos == 0 || nneg == 0 ) 
				return 0;
			//System.out.println(" npos = " + npos + " nneg = " + nneg + "\n");

			for( int iter = 0; iter < max_iter; ){
				double PGmax_new = Double.NEGATIVE_INFINITY, PGmin_new = Double.POSITIVE_INFINITY;

				for( int i = 0; i < active_size; ++i ){
					int j = generator.nextInt(active_size-i) ;
					int temp = index[i];
					index[i] = index[j];
					index[j] = temp;
				}

				for( int s = 0; s < active_size; ++s ) {

					int i = index[s];
					Example E = data.get(i);

					double G = 0;
					for( int j = 0; j < E.fids.length; ++j) G += param.weightvector[ E.fids[j] ] * E.fvals[j];
					if( use_bias ) G += param.weightvector[0] * 1;
					G = G * label[i] - 1;

					double PG = 0;
					if( Math.abs( alpha[i]) < EPS ){
						if( G > PGmax_old ) {
							active_size--;
							int temp = index[s];
							index[s] = index[active_size];
							index[active_size] = temp;
							s--;
							continue;
						}
						else if( G < 0 ) PG = G;
					}
					else if( Math.abs(alpha[i]-C) < EPS ){
						if( G < PGmin_old ){
							active_size--;
							int temp = index[s];
							index[s] = index[active_size];
							index[active_size] = temp;
							s--;
							continue;
						}
						else if( G > 0 ) PG = 0;
					}
					else PG = G;

					PGmax_new = Math.max( PGmax_new , PG );
					PGmin_new = Math.min( PGmin_new , PG );

					if( Math.abs(PG) > 1e-12 ){
						double alpha_old = alpha[i];
						alpha[i] = Math.min( Math.max( alpha[i] - G/QD[i] , 0.0 ) , C );
						double d = ( alpha[i] - alpha_old ) * label[i];
						for( int j = 0; j < E.fids.length; ++j ) param.weightvector[ E.fids[j] ] += d * E.fvals[j];
						if( use_bias ) param.weightvector[0] += d * 1;
					}
				}

				iter++;

				if( iter%10 == 0) System.out.print("..");

				if( PGmax_new - PGmin_new <= eps ){
					if( active_size == N ) break;
					active_size = N;
					System.out.print("*");
					PGmax_old = Double.POSITIVE_INFINITY;
					PGmin_old = Double.NEGATIVE_INFINITY;
					continue;
				}

				PGmax_old = PGmax_new;
				PGmin_old = PGmin_new;

				if( PGmax_old <= 0 ) PGmax_old = Double.POSITIVE_INFINITY;
				if( PGmin_old >= 0 ) PGmin_old = Double.NEGATIVE_INFINITY;
			}

			System.out.println("\n Done ");
			
			double F = 0;
			for ( int i = 0; i < param.weightvector.length; ++i )
				F += param.weightvector[i] * param.weightvector[i];
			F = F/2;
			for ( int i = 0; i < N; ++i )
				F += -alpha[i];
			F = -F;
			return F;
		}
}
