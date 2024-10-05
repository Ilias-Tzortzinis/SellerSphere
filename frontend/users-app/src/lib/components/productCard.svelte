<script lang="ts">
  import type { ProductView } from '$lib/data';
import { Card, Button } from 'flowbite-svelte'

export let product: ProductView;
</script>

<Card padding="none" >
    <a href="/">
        <img src={product.image} alt={product.productName}>
    </a>
    <div class="px-5 pb-5">
        <a href="/">
            <h5 class="text-xl font-semibold tracking-tight text-gray-900 dark:text-white">{product.productName}</h5>
        </a>
        <div class="flex justify-between items-center">
            <span class="text-3xl font-bold text-gray-900 dark:text-white">${product.price / 100}</span>
            <Button href="/" class=" bg-primary-600">Buy now</Button>
        </div>
    </div>
</Card>