<script lang="ts">
  import { PUBLIC_PRODUCT_SERVICE_URL } from "$env/static/public";
import type { ProductQuery, ProductView } from "$lib/data";
  import { onMount } from "svelte";
  import ProductCard from "./productCard.svelte";

export let query: ProductQuery;

let products: ProductView[] = []
let hasMoreProducts = true
let loadingSpinner: HTMLElement;

async function fetchMoreProducts(){
    let urlQuery = query.toUrlQuery()
    if (products.length > 0){
        let lastId = 'lastId='.concat(products[products.length - 1].productId)
        if (urlQuery.length === 0){
            urlQuery = lastId
        }
        else urlQuery = urlQuery + '&' + lastId
    }
    const response = await fetch(`${PUBLIC_PRODUCT_SERVICE_URL}/products/search/${query.category()}?${urlQuery}`, {
        method: 'GET',
        headers: {
            'Accept': 'application/json'
        }
    })
    console.log(response.url)
    const result: ProductView[] = await response.json()
    products = [...products, ...result];
    // hasMoreProducts = result.length === 15;
}

let observer: IntersectionObserver;

onMount(() => {
    observer = new IntersectionObserver(entries => {
        if (entries[0].isIntersecting){
            fetchMoreProducts()
        }
    })
    observer.observe(loadingSpinner)

     return () => observer.disconnect()
})
</script>

<div class="grid grid-cols-3 gap-4 items-center justify-items-center">
    {#each products as product}
    <ProductCard {product} />
    {/each}
</div>
<div bind:this={loadingSpinner} >
    <p>Loading...</p>
</div>