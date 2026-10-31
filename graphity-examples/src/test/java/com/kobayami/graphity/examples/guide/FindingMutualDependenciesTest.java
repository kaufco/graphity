package com.kobayami.graphity.examples.guide;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class FindingMutualDependenciesTest {

  @Test
  void theStatementsAndTheExpressionsAreTheRecursiveGroups() {
    FindingMutualDependencies.Groups groups = FindingMutualDependencies.recursiveGroups();
    assertThat(groups.sccCount()).isEqualTo(6);
    assertThat(groups.recursive()).containsExactly(
        List.of("expression", "term", "factor"), List.of("statement", "block", "ifStatement"));
  }

  @Test
  void factorAndExpressionUseEachOtherButAssignmentOnlyUsesExpression() {
    FindingMutualDependencies.SameGroup sameGroup = FindingMutualDependencies.sameGroup();
    assertThat(sameGroup.expressions()).isEqualTo(2);
    assertThat(sameGroup.factorInIt()).isTrue();
    assertThat(sameGroup.assignmentInIt()).isFalse();
  }
}
