
#pragma omp parallel for schedule(dynamic, 10) reduction(+:acertos)
for (int i = 0; i < TEST_SAMPLES; i++) {
    if (real == pred) {
        acertos++;  // SEGURO
    }
}


