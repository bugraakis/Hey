
public class CircularQueue {

    Object[] elements;
    int rear, front;

    public CircularQueue(int capacity) {
        elements = new Object[capacity];
        rear  = -1;
        front = 0;
    }

    public void Enqueue(Object data) {
        rear = (rear + 1) % elements.length; // wrap around
        elements[rear] = data;
    }

    public Object Dequeue() {
        if (isEmpty()) {
            System.out.println("Queue is empty");
            return null;
        }
        Object data = elements[front];
        elements[front] = null;
        front = (front + 1) % elements.length;
        return data;
    }

    public Object Peek() {
        return elements[front];
    }

    public boolean isFull() {
        return front == (rear + 1) % elements.length
                && elements[front] != null
                && elements[rear]  != null;
    }

    public boolean isEmpty() {
        return elements[front] == null;
    }

    public int Size() {
        if (elements[front] == null) return 0;
        if (rear >= front) return rear - front + 1;
        return elements.length - (front - rear) + 1; // wrapped case
    }
}