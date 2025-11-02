package memorymonitoring.runtime;

sealed interface Reference permits FieldReference, ArrayReference {
}
