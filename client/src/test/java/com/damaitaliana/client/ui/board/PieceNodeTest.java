package com.damaitaliana.client.ui.board;

import static org.assertj.core.api.Assertions.assertThat;

import com.damaitaliana.shared.domain.Color;
import com.damaitaliana.shared.domain.Piece;
import com.damaitaliana.shared.domain.PieceKind;
import com.damaitaliana.shared.domain.Square;
import javafx.scene.Node;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import org.junit.jupiter.api.Test;

/**
 * Pure-Java assertions on {@link PieceNode}'s style classes and node tree (Task 3.5.7). JavaFX
 * shapes can be instantiated without a running toolkit, so these tests don't need {@code
 * Platform.startup} — they only check {@code getStyleClass()} membership and child types.
 */
class PieceNodeTest {

  private static final Square ANY = new Square(0, 0);

  @Test
  void whiteManBodyHasPieceClassButNoKingOrBlackClass() {
    PieceNode node = new PieceNode(new Piece(Color.WHITE, PieceKind.MAN), ANY);
    Circle body = (Circle) node.getChildren().get(0);
    assertThat(body.getStyleClass()).contains("piece").doesNotContain("piece-black", "piece-king");
    assertThat(textChildren(node)).isEmpty();
  }

  @Test
  void blackManBodyHasPieceAndPieceBlackButNoKingMarker() {
    PieceNode node = new PieceNode(new Piece(Color.BLACK, PieceKind.MAN), ANY);
    Circle body = (Circle) node.getChildren().get(0);
    assertThat(body.getStyleClass()).contains("piece", "piece-black").doesNotContain("piece-king");
    assertThat(textChildren(node)).isEmpty();
  }

  @Test
  void whiteKingHasGoldKingMarkerAndPieceKingClassOnBody() {
    PieceNode node = new PieceNode(new Piece(Color.WHITE, PieceKind.KING), ANY);
    Circle body = (Circle) node.getChildren().get(0);
    assertThat(body.getStyleClass()).contains("piece", "piece-king").doesNotContain("piece-black");
    Text crown = textChildren(node).get(0);
    assertThat(crown.getStyleClass())
        .contains("piece-king-marker")
        .doesNotContain("piece-king-marker-black");
    assertThat(crown.getText()).isEqualTo(PieceNode.CROWN_GLYPH);
  }

  @Test
  void blackKingHasDeepRedKingMarkerAndPieceBlackPieceKingOnBody() {
    PieceNode node = new PieceNode(new Piece(Color.BLACK, PieceKind.KING), ANY);
    Circle body = (Circle) node.getChildren().get(0);
    assertThat(body.getStyleClass()).contains("piece", "piece-black", "piece-king");
    Text crown = textChildren(node).get(0);
    assertThat(crown.getStyleClass()).contains("piece-king-marker", "piece-king-marker-black");
  }

  @Test
  void manHasNoTextChildAndKingHasExactlyOne() {
    PieceNode man = new PieceNode(new Piece(Color.WHITE, PieceKind.MAN), ANY);
    PieceNode king = new PieceNode(new Piece(Color.WHITE, PieceKind.KING), ANY);
    assertThat(textChildren(man)).isEmpty();
    assertThat(textChildren(king)).hasSize(1);
  }

  private static java.util.List<Text> textChildren(PieceNode node) {
    return node.getChildren().stream()
        .filter(Text.class::isInstance)
        .map(Text.class::cast)
        .collect(java.util.stream.Collectors.toList());
  }

  @SuppressWarnings("unused")
  private static Node firstChild(PieceNode node) {
    return node.getChildren().get(0);
  }
}
