package deplambda.parser;

import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import deplambda.util.Sentence;
import deplambda.util.TransformationRuleGroups;
import edu.cornell.cs.nlp.spf.mr.lambda.FlexibleTypeComparator;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicLanguageServices;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.SimpleLogicalExpressionReader;
import edu.cornell.cs.nlp.spf.mr.language.type.MutableTypeRepository;
import edu.cornell.cs.nlp.utils.composites.Pair;

public class TreeTransformerUniversalTest {

  static TransformationRuleGroups treeTransformationRules;
  static TransformationRuleGroups lambdaAssignmentRules;
  static TransformationRuleGroups relationRules;
  static MutableTypeRepository types;
  static JsonParser jsonParser = new JsonParser();

  static {
    try {
      types = new MutableTypeRepository("lib_data/ud.types.txt");

      LogicLanguageServices.setInstance(new LogicLanguageServices.Builder(
          types, new FlexibleTypeComparator()).closeOntology(false)
          .setNumeralTypeName("i").build());

      treeTransformationRules =
          new TransformationRuleGroups(
              "lib_data/ud-tree-transformation-rules.proto.txt");
      relationRules =
          new TransformationRuleGroups(
              "lib_data/ud-relation-priorities.proto.txt");
      lambdaAssignmentRules =
          new TransformationRuleGroups(
              "lib_data/ud-lambda-assignment-rules.proto.txt");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Test
  public final void testTypes() {
    assertEquals("<<<a,e>,t>,<<<a,e>,t>,<<a,e>,t>>>",
        types.unfoldType(types.getType("r")));
  }

  @Test
  public final void testPunct() {
    JsonObject jsonSentence =
        jsonParser
            .parse(
                "{\"sentence\":\"Yahoo!\",\"words\":[{\"word\":\"Yahoo\",\"lemma\":\"yahoo\",\"pos\":\"PROPN\",\"ner\":\"ORGANIZATION\",\"dep\":\"root\",\"head\":0,\"index\":1},{\"word\":\"!\",\"lemma\":\"!\",\"pos\":\"PUNCT\",\"ner\":\"O\",\"index\":2,\"head\":1,\"dep\":\"punct\",\"sentEnd\":true}]}")
            .getAsJsonObject();
    Sentence sentence = new Sentence(jsonSentence);

    // TreeTransformationRules for modifying the structure of a tree.
    TreeTransformer.applyRuleGroupsOnTree(treeTransformationRules,
        sentence.getRootNode());
    assertEquals("(l-root w-1-yahoo t-PROPN (l-punct w-2-! t-PUNCT))", sentence
        .getRootNode().toString());

    String binarizedTreeString =
        TreeTransformer.binarizeTree(sentence.getRootNode(),
            relationRules.getRelationPriority());
    assertEquals("(l-punct w-1-yahoo w-2-!)", binarizedTreeString);

    // Assign lambdas.
    TreeTransformer.applyRuleGroupsOnTree(lambdaAssignmentRules,
        sentence.getRootNode());

    // Composing lambda.
    Pair<String, List<LogicalExpression>> sentenceSemantics =
        TreeTransformer.composeSemantics(sentence.getRootNode(),
            relationRules.getRelationPriority(), false);

    assertEquals(
        "(lambda $0:<a,e> (and:c (and:c (p_TYPE_w-1-yahoo:u $0) (p_EVENT_w-1-yahoo:u $0) (p_EVENT.ENTITY_arg1:b $0 $0)) (p_EMPTY:u $0)))",
        sentenceSemantics.second().get(0).toString());
  }

  @Test
  public final void testCase() {
    JsonObject jsonSentence =
        jsonParser
            .parse(
                "{\"sentence\":\"in India.\",\"words\":[{\"word\":\"in\",\"lemma\":\"in\",\"pos\":\"ADP\",\"ner\":\"O\",\"index\":1,\"head\":2,\"dep\":\"case\"},{\"word\":\"India\",\"lemma\":\"india\",\"pos\":\"PROPN\",\"ner\":\"LOCATION\",\"dep\":\"root\",\"head\":0,\"index\":2},{\"word\":\".\",\"lemma\":\".\",\"pos\":\"PUNCT\",\"ner\":\"O\",\"index\":3,\"head\":2,\"dep\":\"punct\",\"sentEnd\":true}]}")
            .getAsJsonObject();
    Sentence sentence = new Sentence(jsonSentence);

    // TreeTransformationRules for modifying the structure of a tree.
    TreeTransformer.applyRuleGroupsOnTree(treeTransformationRules,
        sentence.getRootNode());
    assertEquals(
        "(l-root w-2-india t-PROPN (l-case w-1-in t-ADP) (l-punct w-3-. t-PUNCT))",
        sentence.getRootNode().toString());

    String binarizedTreeString =
        TreeTransformer.binarizeTree(sentence.getRootNode(),
            relationRules.getRelationPriority());
    assertEquals("(l-punct (l-case w-2-india w-1-in) w-3-.)",
        binarizedTreeString);

    // Assign lambdas.
    TreeTransformer.applyRuleGroupsOnTree(lambdaAssignmentRules,
        sentence.getRootNode());

    // Composing lambda.
    Pair<String, List<LogicalExpression>> sentenceSemantics =
        TreeTransformer.composeSemantics(sentence.getRootNode(),
            relationRules.getRelationPriority(), false);

    assertEquals(
        "(lambda $0:<a,e> (and:c (and:c (and:c (p_TYPE_w-2-india:u $0) (p_EVENT_w-2-india:u $0) (p_EVENT.ENTITY_arg1:b $0 $0)) (p_EMPTY:u $0)) (p_EMPTY:u $0)))",
        sentenceSemantics.second().get(0).toString());
  }

  @Test
  public final void testNmod() {
    // nmod with a case attached to a noun.
    JsonObject jsonSentence =
        jsonParser
            .parse(
                "{\"sentence\":\"city in India .\",\"words\":[{\"word\":\"city\",\"lemma\":\"city\",\"pos\":\"NOUN\",\"ner\":\"O\",\"dep\":\"root\",\"head\":0,\"index\":1},{\"word\":\"in\",\"lemma\":\"in\",\"pos\":\"ADP\",\"ner\":\"O\",\"index\":2,\"head\":3,\"dep\":\"case\"},{\"word\":\"India\",\"lemma\":\"india\",\"pos\":\"PROPN\",\"ner\":\"LOCATION\",\"index\":3,\"head\":1,\"dep\":\"nmod\"},{\"word\":\".\",\"lemma\":\".\",\"pos\":\"PUNCT\",\"ner\":\"O\",\"index\":4,\"head\":1,\"dep\":\"punct\",\"sentEnd\":true}]}")
            .getAsJsonObject();
    Sentence sentence = new Sentence(jsonSentence);

    // TreeTransformationRules for modifying the structure of a tree.
    TreeTransformer.applyRuleGroupsOnTree(treeTransformationRules,
        sentence.getRootNode());
    assertEquals(
        "(l-root w-1-city t-NOUN (l-nmod w-3-india t-PROPN (l-case w-2-in t-ADP)) (l-punct w-4-. t-PUNCT))",
        sentence.getRootNode().toString());

    String binarizedTreeString =
        TreeTransformer.binarizeTree(sentence.getRootNode(),
            relationRules.getRelationPriority());
    assertEquals("(l-punct (l-nmod w-1-city (l-case w-3-india w-2-in)) w-4-.)",
        binarizedTreeString);

    // Assign lambdas.
    TreeTransformer.applyRuleGroupsOnTree(lambdaAssignmentRules,
        sentence.getRootNode());

    // Composing lambda.
    Pair<String, List<LogicalExpression>> sentenceSemantics =
        TreeTransformer.composeSemantics(sentence.getRootNode(),
            relationRules.getRelationPriority(), false);

    assertEquals(
        "(lambda $0:<a,e> (and:c (exists:ex $1:<a,e> (and:c (and:c (p_TYPE_w-1-city:u $0) (p_EVENT_w-1-city:u $0) (p_EVENT.ENTITY_arg1:b $0 $0)) (and:c (and:c (p_TYPE_w-3-india:u $1) (p_EVENT_w-3-india:u $1) (p_EVENT.ENTITY_arg1:b $1 $1)) (p_EMPTY:u $1)) (p_EVENT.ENTITY_l-nmod.w-2-in:b $0 $1))) (p_EMPTY:u $0)))",
        sentenceSemantics.second().get(0).toString());

    // nmod with a case attached to a verb.
    jsonSentence =
        jsonParser
            .parse(
                "{\"sentence\":\"saw with telescope .\",\"words\":[{\"word\":\"saw\",\"lemma\":\"see\",\"pos\":\"VERB\",\"ner\":\"O\",\"dep\":\"root\",\"head\":0,\"index\":1},{\"word\":\"with\",\"lemma\":\"with\",\"pos\":\"ADP\",\"ner\":\"O\",\"index\":2,\"head\":3,\"dep\":\"case\"},{\"word\":\"telescope\",\"lemma\":\"telescope\",\"pos\":\"NOUN\",\"ner\":\"O\",\"index\":3,\"head\":1,\"dep\":\"nmod\"},{\"word\":\".\",\"lemma\":\".\",\"pos\":\"PUNCT\",\"ner\":\"O\",\"index\":4,\"head\":1,\"dep\":\"punct\",\"sentEnd\":true}]}")
            .getAsJsonObject();
    sentence = new Sentence(jsonSentence);

    // TreeTransformationRules for modifying the structure of a tree.
    TreeTransformer.applyRuleGroupsOnTree(treeTransformationRules,
        sentence.getRootNode());
    assertEquals(
        "(l-root w-1-see t-VERB (l-nmod w-3-telescope t-NOUN (l-case w-2-with t-ADP)) (l-punct w-4-. t-PUNCT))",
        sentence.getRootNode().toString());

    binarizedTreeString =
        TreeTransformer.binarizeTree(sentence.getRootNode(),
            relationRules.getRelationPriority());
    assertEquals(
        "(l-punct (l-nmod w-1-see (l-case w-3-telescope w-2-with)) w-4-.)",
        binarizedTreeString);

    // Assign lambdas.
    TreeTransformer.applyRuleGroupsOnTree(lambdaAssignmentRules,
        sentence.getRootNode());

    // Composing lambda.
    sentenceSemantics =
        TreeTransformer.composeSemantics(sentence.getRootNode(),
            relationRules.getRelationPriority(), false);

    assertEquals(
        "(lambda $0:<a,e> (and:c (exists:ex $1:<a,e> (and:c (p_EVENT_w-1-see:u $0) (and:c (and:c (p_TYPE_w-3-telescope:u $1) (p_EVENT_w-3-telescope:u $1) (p_EVENT.ENTITY_arg1:b $1 $1)) (p_EMPTY:u $1)) (p_EVENT.ENTITY_l-nmod.w-2-with:b $0 $1))) (p_EMPTY:u $0)))",
        sentenceSemantics.second().get(0).toString());

    // TODO nmod with possessive case.
    jsonSentence =
        jsonParser
            .parse(
                "{\"sentence\":\"Darwin's book\",\"words\":[{\"word\":\"Darwin\",\"lemma\":\"darwin\",\"pos\":\"PROPN\",\"ner\":\"PERSON\",\"index\":1,\"head\":3,\"dep\":\"nmod:poss\"},{\"word\":\"'s\",\"lemma\":\"'s\",\"pos\":\"PART\",\"ner\":\"O\",\"index\":2,\"head\":1,\"dep\":\"case\"},{\"word\":\"book\",\"lemma\":\"book\",\"pos\":\"NOUN\",\"ner\":\"O\",\"dep\":\"root\",\"head\":0,\"index\":3,\"sentEnd\":true}]}")
            .getAsJsonObject();
    sentence = new Sentence(jsonSentence);

  }

  @Test
  public final void testReader() {
    // Cameron directed Titanic.
    LogicalExpression e1 =
        SimpleLogicalExpressionReader
            .read("(lambda $f:w (lambda $g:w (lambda $x:v (and:c ($f $x) ($g $x)))))");
    LogicalExpression e2 =
        SimpleLogicalExpressionReader
            .read("(lambda $0:<v,t> (lambda $1:<v,t> (lambda $2:<a,e> (and:c ($0 $2) ($1 $2)))))");
    assertTrue(e1.equals(e2));
  }
}
