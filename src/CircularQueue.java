package src;

public class CircularQueue {
    Object[] elements;
    int rear,front;


    public CircularQueue(int Capacity){
        elements = new Object[Capacity];
        rear = -1;
        front = 0;
    }

    public void Enqueue(Object data){
    rear = (rear+1) % elements.length;
    elements[rear] = data;
    }

    public Object Dequeue(){
    if(isEmpty()){
        System.out.println("Queue is empty");
        return null;
    }
       else
       {
        Object data = elements[front];
        elements[front] = null;
        front = (front+1) % elements.length;
        return data;
       }
    }

    public Object Peek(){
    Object data = elements[front];
    return data;
    }

    public boolean isFull(){
        if (front == ( rear + 1) % elements.length &&
                elements[front] != null &&
                elements[rear] != null) return true;
    else return false;
    }
    public boolean isEmpty(){
        return elements[front] == null;
    }
    public int Size(){
    if (elements[front] == null){
        return 0;}
        else{
            if (rear >= front)
                return rear - front + 1;
            else
                return elements.length - (front - rear) + 1;
        }
    }
}
