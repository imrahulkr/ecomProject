package com.ecommerce.project.product;

import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.category.Category;
import com.ecommerce.project.product.dto.ProductDTO;
import com.ecommerce.project.product.dto.ProductResponse;
import com.ecommerce.project.category.CategoryRepository;
import com.ecommerce.project.service.FileService;
import com.ecommerce.project.util.AuthUtil;
import com.ecommerce.project.auth.User;
import com.ecommerce.project.auth.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final FileService fileService;
    private final AuthUtil authUtil;

    @Value("${project.image}")
    private String path;

    @Value("${image.base.url}")
    private String imageBaseUrl;

    @Value("${app.currency}")
    private String currency;

    // @Transactional on every mutating method below: each does a fetch-then-save, and without an
    // open session spanning both, save() on the by-then-detached entity forces Hibernate through
    // merge() instead of a plain managed-entity flush - which is what surfaced a real Hibernate
    // bug here (a ConcurrentModificationException deep in loading the associated User's lazy
    // `products` Set while merge() reconciles that association). Keeping the entity attached the
    // whole time avoids merge() entirely.
    @Override
    @Transactional
    public ProductDTO addProduct(Long categoryId, ProductDTO productDTO) {
        Category category = categoryRepository.findById(categoryId).orElseThrow(()-> new ResourceNotFoundException("Category", "categoryId", categoryId));
        Product product = modelMapper.map(productDTO, Product.class);
        Product existingProduct = productRepository.findByProductNameIgnoreCase(product.getProductName());
        if(existingProduct != null) throw new APIException("Product with this Name already exists :::: " + product.getProductName());
        product.setImage("Default.png");
        product.setCategory(category);
        product.setUser(authUtil.loggedInUser());
        product.setCurrency(currency);
        product.setSpecialPriceMinorUnits(Math.round(product.getPriceMinorUnits() - (product.getDiscount() * product.getPriceMinorUnits()) / 100));
        Product savedProduct = productRepository.save(product);
        return toProductDTO(savedProduct);
    }

    @Override
    public ProductResponse getAllProducts(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder, String keyword, String category) {
        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);

        Specification<Product> spec = Specification.unrestricted();
        if(keyword != null && !keyword.isEmpty()){
            spec = spec.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("productName")), "%" + keyword.toLowerCase() + "%"));
        }

        if(category != null && !category.isEmpty()){
            spec = spec.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(root.get("category").get("categoryName"), category));
        }

        Page<Product> productPage = productRepository.findAll(spec, pageDetails);
        return getProductResponseFromProductPage(productPage, pageDetails.getPageNumber());
    }

    @Override
    public ProductResponse getAllProductByCategories(Long categoryId, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        Category category = categoryRepository.findById(categoryId).orElseThrow(()-> new ResourceNotFoundException("Category", "categoryId", categoryId));
        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
        Page<Product> productPage = productRepository.findByCategory(category, pageDetails);
        return getProductResponseFromProductPage(productPage, pageDetails.getPageNumber());
    }

    @Override
    public ProductResponse getProductByKeyword(String keyword, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
        Page<Product> productPage = productRepository.findByProductNameLikeIgnoreCase('%'+keyword+'%', pageDetails);
        return getProductResponseFromProductPage(productPage, pageDetails.getPageNumber());
    }

    @Override
    public ProductDTO getProductById(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));
        ProductDTO productDTO = toProductDTO(product);
        productDTO.setImage(constructImageUrl(product.getImage()));
        return productDTO;
    }

    @Override
    @Transactional
    public ProductDTO updateProduct(Long productId, ProductDTO productDTO) {
        Product existingProduct = productRepository.findById(productId).orElseThrow(()-> new ResourceNotFoundException("Product", "productId", productId));
        applyProductEdits(existingProduct, productDTO);
        Product updatedProduct = productRepository.save(existingProduct);
        return toProductDTO(updatedProduct);
    }

    @Override
    @Transactional
    public ProductDTO deleteProduct(Long productId) {
        Product existingProduct = productRepository.findById(productId).orElseThrow(()-> new ResourceNotFoundException("Product", "productId", productId));
        productRepository.delete(existingProduct);

        return toProductDTO(existingProduct);
    }

    @Override
    @Transactional
    public ProductDTO updateProductImage(Long productId, MultipartFile image) throws IOException {
        // Get Product from DB
        Product existingProduct = productRepository.findById(productId).orElseThrow(()-> new ResourceNotFoundException("Product", "productId", productId));
        // Upload Image to server (in /image folder) and get the file name of the uploaded image
        String fileName = fileService.uploadImage(path, image);
        // Updating the new file name to the product
        existingProduct.setImage(fileName);
        // Save the updated product
        Product updateProduct = productRepository.save(existingProduct);
        return toProductDTO(updateProduct);
    }

    // Shared by updateProduct/updateProductAsSeller. Category is looked up explicitly by id rather
    // than trusting modelMapper.map(productDTO, Product.class) to populate the Category association
    // from a bare categoryId - ModelMapper's implicit nested-property matching for a non-DTO-shaped
    // association isn't reliable and previously left existingProduct.category silently unset.
    // specialPriceMinorUnits is likewise never taken from the client - it's recomputed here the same
    // way addProduct computes it, so a price/discount edit can't drift the two apart.
    private void applyProductEdits(Product existingProduct, ProductDTO productDTO) {
        Category category = categoryRepository.findById(productDTO.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "categoryId", productDTO.getCategoryId()));
        existingProduct.setPriceMinorUnits(productDTO.getPriceMinorUnits());
        existingProduct.setDiscount(productDTO.getDiscount());
        existingProduct.setSpecialPriceMinorUnits(Math.round(productDTO.getPriceMinorUnits() - (productDTO.getDiscount() * productDTO.getPriceMinorUnits()) / 100));
        existingProduct.setQuantity(productDTO.getQuantity());
        existingProduct.setCategory(category);
        existingProduct.setProductName(productDTO.getProductName());
        existingProduct.setDescription(productDTO.getDescription());
    }

    private String constructImageUrl(String imageName) {
        return imageBaseUrl.endsWith("/") ? imageBaseUrl + imageName : imageBaseUrl + "/" + imageName;
    }

    @Override
    public ProductResponse getAllProductsForAdmin(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
        Page<Product> productPage = productRepository.findAll(pageDetails);
        return getProductResponseFromProductPage(productPage, pageDetails.getPageNumber());
    }

    @Override
    public ProductResponse getAllProductForSeller(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
        Page<Product> productPage = productRepository.findByUser(authUtil.loggedInUser(), pageDetails);
        return getProductResponseFromProductPage(productPage, pageDetails.getPageNumber());
    }

    // Admin-only viewing path: no ownership restriction on which sellerId is passed in, unlike
    // getAllProductForSeller above which is always scoped to the caller.
    @Override
    public ProductResponse getAllProductsBySellerId(Long sellerId, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("user", "userId", sellerId));
        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
        Page<Product> productPage = productRepository.findByUser(seller, pageDetails);
        return getProductResponseFromProductPage(productPage, pageDetails.getPageNumber());
    }

    @Override
    @Transactional
    public ProductDTO updateProductAsSeller(Long sellerId, Long productId, ProductDTO productDTO) {
        Product existingProduct = productRepository.findByProductIdAndUser_UserId(productId, sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));
        applyProductEdits(existingProduct, productDTO);
        Product updatedProduct = productRepository.save(existingProduct);
        return toProductDTO(updatedProduct);
    }

    @Override
    @Transactional
    public ProductDTO deleteProductAsSeller(Long sellerId, Long productId) {
        Product existingProduct = productRepository.findByProductIdAndUser_UserId(productId, sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));
        productRepository.delete(existingProduct);
        return toProductDTO(existingProduct);
    }

    @Override
    @Transactional
    public ProductDTO updateProductImageAsSeller(Long sellerId, Long productId, MultipartFile image) throws IOException {
        Product existingProduct = productRepository.findByProductIdAndUser_UserId(productId, sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));
        String fileName = fileService.uploadImage(path, image);
        existingProduct.setImage(fileName);
        Product updateProduct = productRepository.save(existingProduct);
        return toProductDTO(updateProduct);
    }


    /*=======================================================================================================*/
    /*========================================= Helper Methods ==============================================*/
    /*=======================================================================================================*/

    public ProductResponse getProductResponseFromProductPage(Page<Product> productPage, Integer pageNumber){
        List<Product> products = productPage.getContent();

        List<ProductDTO> productDTOS = products.isEmpty() ? Collections.emptyList() : products.stream().map(product ->
        {
            ProductDTO productDTO = toProductDTO(product);
            productDTO.setImage(constructImageUrl(product.getImage()));
            return productDTO;
        })
                .collect(Collectors.toList());
        ProductResponse productResponse = new ProductResponse(
                productDTOS,
                pageNumber,
                productPage.getSize(),
                productPage.getTotalElements(),
                productPage.getTotalPages(),
                productPage.isLast());
        return productResponse;
    }

    private ProductDTO toProductDTO(Product product) {
        ProductDTO productDTO = modelMapper.map(product, ProductDTO.class);
        if (product.getCategory() != null) {
            productDTO.setCategoryId(product.getCategory().getCategoryId());
        }
        if (product.getUser() != null) {
            productDTO.setSellerId(product.getUser().getUserId());
            productDTO.setSellerName(product.getUser().getUsername());
        }
        return productDTO;
    }
}
