/*
 * Copyright 2018 Tommaso Teofili and Simone Tripodi
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.github.tteofili.jtm.tm;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.SingularValueDecomposition;
import org.deeplearning4j.models.embeddings.WeightLookupTable;
import org.deeplearning4j.models.word2vec.VocabWord;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import com.github.tteofili.jtm.feed.jira.Comment;
import com.github.tteofili.jtm.feed.jira.Issue;

/**
 * Utility class for Par2Hier vector algorithms
 */
public class Par2HierUtils {

  public enum Method {
    CLUSTER,
    SUM
  }

  /**
   * transforms paragraph vectors into hierarchical vectors
   * @param iterator iterator over docs
   * @param lookupTable the paragraph vector table
   * @param labels the labels
   * @param k the no. of centroids
   * @return a map doc->hierarchical vector
   */
  static Map<String, INDArray> getPar2Hier(JiraIterator iterator,
                                           WeightLookupTable<VocabWord> lookupTable,
                                           List<String> labels, int k, Method method) {
    Collections.sort(labels);
    Collection<Issue> issues = iterator.getIssues();

    Map<String, INDArray> hvs = new TreeMap<>();
    // for each doc
    for (Issue issue : issues) {
      Par2HierUtils.getPar2HierVector(lookupTable, issue, k, hvs, method);
    }
    return hvs;
  }

  /**
   * base case: on a leaf hv = pv
   * on a non-leaf node with n children: hv = pv + k centroids of the n hv
   */
  private static void getPar2HierVector(WeightLookupTable<VocabWord> lookupTable, Issue issue,
                                        int k, Map<String, INDArray> hvs, Method method) {
    if (hvs.containsKey(issue.getKey().getValue())) {
      hvs.get(issue.getKey().getValue());
      return;
    }
    INDArray hv = lookupTable.vector(issue.getKey().getValue());

    List<Comment> comments = issue.getComments();
    if (comments.size() == 0) {
      // just the pv
      hvs.put(issue.getKey().getValue(), hv);
    } else {
      INDArray chvs = Nd4j.zeros(comments.size(), hv.columns());
      int i = 0;
      for (Comment desc : comments) {
        // child hierarchical vector
        INDArray chv = lookupTable.vector(desc.getId());
        chvs.putRow(i, chv);
        i++;
      }

      double[][] centroids;
      if (chvs.rows() > k) {
        centroids = Par2HierUtils.getTruncatedVT(chvs, k);
      } else if (chvs.rows() == 1) {
        centroids = Par2HierUtils.getDoubles(chvs.getRow(0));
      } else {
        centroids = Par2HierUtils.getTruncatedVT(chvs, 1);
      }
      switch (method) {
        case CLUSTER:
          INDArray matrix = Nd4j.zeros(centroids.length + 1, hv.columns());
          matrix.putRow(0, hv);
          for (int c = 0; c < centroids.length; c++) {
            matrix.putRow(c + 1, Nd4j.create(centroids[c]));
          }
          hv = Nd4j.create(Par2HierUtils.getTruncatedVT(matrix, 1));
          break;
        case SUM:
          for (double[] centroid : centroids) {
            hv.addi(Nd4j.create(centroid));
          }
          break;
      }

      hvs.put(issue.getKey().getValue(), hv);
    }
  }

  private static double[][] getTruncatedVT(INDArray matrix, int k) {
    double[][] data = getDoubles(matrix);

    SingularValueDecomposition svd = new SingularValueDecomposition(MatrixUtils.createRealMatrix(data));

    double[][] truncatedVT = new double[k][svd.getVT().getColumnDimension()];
    svd.getVT().copySubMatrix(0, k - 1, 0, truncatedVT[0].length - 1, truncatedVT);
    return truncatedVT;
  }

  private static double[][] getDoubles(INDArray matrix) {
    double[][] data = new double[matrix.rows()][matrix.columns()];
    for (int i = 0; i < data.length; i++) {
      for (int j = 0; j < data[0].length; j++) {
        data[i][j] = matrix.getDouble(i, j);
      }
    }
    return data;
  }

}
