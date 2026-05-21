

public class Stack {
    int top;
    Object[] elements;

    public Stack(int capacity){
        elements = new Object[capacity];
        top = -1;
    }

    public void push(Object data){
        if(isFull()){
            System.out.println("Stack is Full");
        }
        else{
            top++;
            elements[top] = data;
        }
    }

    public Object pop(){
        if(isEmpty()){
            System.out.println("Stack is Empty");
            return null;
        }
        else{
            Object retdata = elements[top];
            top--;
            return retdata;
        }
    }

    public Object peek(){
        if(isEmpty()){
            System.out.println("Stack is Empty");
            return null;
        }
        else{
            return elements[top];
        }
    }

    public boolean isFull(){
        return top + 1 == elements.length;
    }

    public boolean isEmpty(){
        return top == -1;
    }

    public int size(){
        return top+1;
    }

}