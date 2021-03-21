package com.marcolotz.orderbook.core.util;

public class RedBlackNode<T> {

  /** Possible color for this node */
  public static final int BLACK = 0;
  /** Possible color for this node */
  public static final int RED = 1;
  // the key of each node
  public T value;

  /** Parent of node */
  RedBlackNode<T> parent;

  public RedBlackNode<T> getParent() {
    return parent;
  }

  public RedBlackNode<T> getLeft() {
    return left;
  }

  public RedBlackNode<T> getRight() {
    return right;
  }

  /** Left child */
  RedBlackNode<T> left;
  /** Right child */
  RedBlackNode<T> right;
  // the number of elements to the left of each node
  public int numLeft = 0;
  // the number of elements to the right of each node
  public int numRight = 0;
  // the color of a node
  public int color;

  RedBlackNode(){
    color = BLACK;
    numLeft = 0;
    numRight = 0;
    parent = null;
    left = null;
    right = null;
  }

  // Constructor which sets key to the argument.
  RedBlackNode(T value){
    this();
    this.value = value;
  }
}// end class RedBlackNode

