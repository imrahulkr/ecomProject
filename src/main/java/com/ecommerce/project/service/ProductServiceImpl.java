package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.Category;
import com.ecommerce.project.model.Product;
import com.ecommerce.project.model.User;
import com.ecommerce.project.payload.ProductDTO;
import com.ecommerce.project.payload.ProductResponse;
import com.ecommerce.project.repositories.CategoryRepository;
import com.ecommerce.project.repositories.ProductRepository;
import com.ecommerce.project.util.AuthUtil;
import jakarta.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private FileServiceImpl fileServiceImpl;
    @Autowired
    private AuthUtil authUtil;
    @Value("${project.image}")
    String path;

    @Value("${image.base.url}")
    String imageBaseUrl;
    @Override
    public ProductDTO addProduct(@Valid Long categoryId, ProductDTO productDTO) {
        Category category = categoryRepository.findById(categoryId).orElseThrow(()-> new ResourceNotFoundException("Category", "categoryId", categoryId));
        Product product = modelMapper.map(productDTO, Product.class);
        Product existingProduct = productRepository.findByProductNameIgnoreCase(product.getProductName());
        if(existingProduct != null) throw new APIException("Product with this Name already exists :::: " + product.getProductName());
        product.setImage("Default.png");
        product.setCategory(category);
        product.setUser(authUtil.loggedInUser());
        product.setSpecialPrice(product.getPrice() - (product.getDiscount() * product.getPrice())/100);
        Product savedProduct = productRepository.save(product);
        return modelMapper.map(savedProduct, ProductDTO.class);
    }

    @Override
    public ProductResponse getAllProducts(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder, String keyword, String category) {
        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        //Pageable pageDetails = PageRequest.of(pageNumber, pageSize);
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
//        List<Product> products = productPage.getContent();
//        //List<Product> products = productRepository.findAll();
//
//        if(products.isEmpty()) throw new APIException("No Products Created till  Now!!!!");
//        List<ProductDTO> productDTOS = products.stream().map(product -> modelMapper.map(product, ProductDTO.class))
//                .collect(Collectors.toList());
//        ProductResponse productResponse = new ProductResponse();
//        productResponse.setContent(productDTOS);
//        productResponse.setPageNumber(pageDetails.getPageNumber());
//        productResponse.setPageSize(productPage.getSize());
//        productResponse.setTotalPages(productPage.getTotalPages());
//        productResponse.setTotalElement(productPage.getTotalElements());
//        productResponse.setLastPage(productPage.isLast());
        ProductResponse productResponse = getProductResponseFromProductPage(productPage, pageDetails.getPageNumber());
        return productResponse;
    }

    @Override
    public ProductResponse getAllProductByCategories(Long categoryId, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        Category category = categoryRepository.findById(categoryId).orElseThrow(()-> new ResourceNotFoundException("Category", "categoryId", categoryId));
//        List<Product> products = productRepository.findByCategory(category);
//        if(products.isEmpty()) throw new APIException("No Products Present with categoryId : "+categoryId+" !!!!");
//        List<ProductDTO> productDTOS = products.stream().map(product -> modelMapper.map(product, ProductDTO.class))
//                .collect(Collectors.toList());
//        ProductResponse productResponse = new ProductResponse();
//        productResponse.setContent(productDTOS);
        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
        Page<Product> productPage = productRepository.findByCategory(category, pageDetails);
        ProductResponse productResponse = getProductResponseFromProductPage(productPage, pageDetails.getPageNumber());
        return productResponse;
    }




    @Override
    public ProductResponse getProductByKeyword(String keyword, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
//        List<Product> products = productRepository.findByProductNameLikeIgnoreCase('%'+keyword+'%');
//        if(products.isEmpty()) throw new APIException("No Products Found having keyword : "+keyword+" !!!!");
//        List<ProductDTO> productDTOS = products.stream().map(product -> modelMapper.map(product, ProductDTO.class))
//                .collect(Collectors.toList());
//        ProductResponse productResponse = new ProductResponse();
//        productResponse.setContent(productDTOS);
        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
        Page<Product> productPage = productRepository.findByProductNameLikeIgnoreCase('%'+keyword+'%', pageDetails);
        ProductResponse productResponse = getProductResponseFromProductPage(productPage, pageDetails.getPageNumber());
        return productResponse;
    }

    @Override
    public ProductDTO updateProduct(Long productId, ProductDTO productDTO) {
        Product existingProduct = productRepository.findById(productId).orElseThrow(()-> new ResourceNotFoundException("Product", "productId", productId));
        Product product = modelMapper.map(productDTO, Product.class);
        existingProduct.setSpecialPrice(product.getSpecialPrice());
        existingProduct.setPrice(product.getPrice());
        existingProduct.setCategory(product.getCategory());
        existingProduct.setProductName(product.getProductName());
        existingProduct.setDescription(product.getDescription());
        Product updatedProduct = productRepository.save(existingProduct);
        ProductDTO updatedProductDTO = modelMapper.map(updatedProduct, ProductDTO.class);
        return updatedProductDTO;
    }

    @Override
    public ProductDTO deleteProduct(Long productId) {
        Product existingProduct = productRepository.findById(productId).orElseThrow(()-> new ResourceNotFoundException("Product", "productId", productId));
        productRepository.delete(existingProduct);

        return modelMapper.map(existingProduct, ProductDTO.class);
    }

    @Override
    public ProductDTO updateProductImage(Long productId, MultipartFile image) throws IOException {
        // Get Product from DB
        Product existingProduct = productRepository.findById(productId).orElseThrow(()-> new ResourceNotFoundException("Product", "productId", productId));
        // Upload Image to server (in /image folder)
        // Get the file name of uploaded image
        String fileName = fileServiceImpl.uploadImage(path, image);
        // Updating the new file name to the product
        existingProduct.setImage(fileName);
        // Save the updated product
        Product updateProduct = productRepository.save(existingProduct);
        return modelMapper.map(updateProduct, ProductDTO.class);
    }

    private String constructImageUrl(String imageName) {
        return imageBaseUrl.endsWith("/") ? imageBaseUrl + imageName : imageBaseUrl + "/" + imageName;
    }

    @Override
    public ProductResponse getAllProductsForAdmin(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        //Pageable pageDetails = PageRequest.of(pageNumber, pageSize);
        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
        Page<Product> productPage = productRepository.findAll(pageDetails);
//        List<Product> products = productPage.getContent();
//        //List<Product> products = productRepository.findAll();
//
//        if(products.isEmpty()) throw new APIException("No Products Created till  Now!!!!");
//        List<ProductDTO> productDTOS = products.stream().map(product -> modelMapper.map(product, ProductDTO.class))
//                .collect(Collectors.toList());
//        ProductResponse productResponse = new ProductResponse();
//        productResponse.setContent(productDTOS);
//        productResponse.setPageNumber(pageDetails.getPageNumber());
//        productResponse.setPageSize(productPage.getSize());
//        productResponse.setTotalPages(productPage.getTotalPages());
//        productResponse.setTotalElement(productPage.getTotalElements());
//        productResponse.setLastPage(productPage.isLast());
        ProductResponse productResponse = getProductResponseFromProductPage(productPage, pageDetails.getPageNumber());
        return productResponse;
    }

    @Override
    public ProductResponse getAllProductForSeller(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        //Pageable pageDetails = PageRequest.of(pageNumber, pageSize);
        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
        User seller = authUtil.loggedInUser();
        System.out.println("Seller : " + seller);
        Page<Product> productPage = productRepository.findByUser(seller, pageDetails);
        ProductResponse productResponse = getProductResponseFromProductPage(productPage, pageDetails.getPageNumber());
        return productResponse;
    }


    /*=======================================================================================================*/
    /*========================================= Helper Methods ==============================================*/
    /*=======================================================================================================*/

    public ProductResponse getProductResponseFromProductPage(Page<Product> productPage, Integer pageNumber){
        List<Product> products = productPage.getContent();
        //List<Product> products = productRepository.findAll();

        if(products.isEmpty()) throw new APIException("No Products Created till  Now!!!!");
        List<ProductDTO> productDTOS = products.stream().map(product ->
        {
            ProductDTO productDTO =  modelMapper.map(product, ProductDTO.class);
            productDTO.setImage(constructImageUrl(product.getImage()));
            return productDTO;
        })
                .collect(Collectors.toList());
        ProductResponse productResponse = new ProductResponse();
        productResponse.setContent(productDTOS);
        productResponse.setPageNumber(pageNumber);
        productResponse.setPageSize(productPage.getSize());
        productResponse.setTotalPages(productPage.getTotalPages());
        productResponse.setTotalElement(productPage.getTotalElements());
        productResponse.setLastPage(productPage.isLast());
        return productResponse;
    }
}