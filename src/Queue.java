package src;

public class Queue {
    private Object[] elements;
    private int front, rear, size;

    public Queue(int capacity) {
        elements = new Object[capacity];
        front = 0;
        rear = -1;
        size = 0;
    }

    public void enqueue(Object data) {
        if (size < elements.length) {
            rear = (rear + 1) % elements.length;
            elements[rear] = data;
            size++;
        }
    }

    public Object dequeue() {
        if (isEmpty()) return null;
        Object data = elements[front];
        front = (front + 1) % elements.length;
        size--;
        return data;
    }

    public Object peek() {
        if (isEmpty()) return null;
        return elements[front];
    }

    public boolean search(Object item) {
        if (isEmpty() || item == null) return false;

        int current = front;
        for (int i = 0; i < size; i++) {
            if (elements[current].equals(item)) {
                return true;
            }
            current = (current + 1) % elements.length;
        }
        return false;
    }

    public boolean isEmpty() { return size == 0; }
    public boolean isFull()  { return size == elements.length; }
    public int size()        { return size; }
}
