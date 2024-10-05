<script lang="ts">

import { page } from "$app/stores";
  import ProductBrowser from "$lib/components/product_browser.svelte";
  import { LaptopsQuery, type ProductView } from "$lib/data";
  import { onMount } from "svelte";


let query = new LaptopsQuery()
</script>

<h1 class=" text-red-500 text-2xl font-bold text-center">Users App</h1>

<ProductBrowser {query} />
